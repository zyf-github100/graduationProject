import argparse
import os
import shutil
import subprocess
import textwrap
from pathlib import Path
from tempfile import TemporaryDirectory

from ssh_jump_auth import connect_jump, load_private_key_from_path, read_private_key_from_jump
import paramiko


PROJECT_ROOT = Path(__file__).resolve().parent.parent
FRONTEND_DIR = PROJECT_ROOT / "frontend"
FRONTEND_STACK_DIR = PROJECT_ROOT / "infra" / "remote-frontend"
DEFAULT_REMOTE_DIR = "/opt/school-erp/stacks/erp-frontend"
DEFAULT_COMPOSE_PROJECT = "school-erp-frontend"
DEFAULT_TARGET_PORT = 18200
DEFAULT_BASTION_PORT = 90
DEFAULT_TARGET_HOST = "36.137.84.162"
DEFAULT_JUMP_HOST = "8.148.181.9"


def run_local(command: list[str], cwd: Path, env: dict[str, str] | None = None) -> None:
    executable = command[0]
    if os.name == "nt" and executable == "npm":
        command = [shutil.which("npm.cmd") or "npm.cmd", *command[1:]]
    completed = subprocess.run(command, cwd=cwd, check=False, env=env)
    if completed.returncode != 0:
        raise RuntimeError(f"Local command failed ({completed.returncode}): {' '.join(command)}")


def run_remote(client: paramiko.SSHClient, command: str) -> str:
    stdin, stdout, stderr = client.exec_command(command)
    exit_status = stdout.channel.recv_exit_status()
    output = stdout.read().decode("utf-8", errors="replace")
    error = stderr.read().decode("utf-8", errors="replace")
    if exit_status != 0:
        raise RuntimeError(
            f"Remote command failed ({exit_status}): {command}\n{output}\n{error}"
        )
    return output


def ensure_remote_dir(sftp: paramiko.SFTPClient, remote_dir: str) -> None:
    parts = remote_dir.strip("/").split("/")
    current = ""
    for part in parts:
        current = f"{current}/{part}"
        try:
            sftp.stat(current)
        except FileNotFoundError:
            sftp.mkdir(current)


def upload_tree(sftp: paramiko.SFTPClient, local_dir: Path, remote_dir: str) -> None:
    ensure_remote_dir(sftp, remote_dir)
    for item in local_dir.iterdir():
        remote_path = f"{remote_dir}/{item.name}"
        if item.is_dir():
            upload_tree(sftp, item, remote_path)
        else:
            sftp.put(str(item), remote_path)


def connect_target(
    jump_client: paramiko.SSHClient,
    target_host: str,
    target_port: int,
    target_user: str,
    private_key: paramiko.PKey,
) -> paramiko.SSHClient:
    channel = jump_client.get_transport().open_channel(
        "direct-tcpip",
        (target_host, target_port),
        ("127.0.0.1", 0),
        timeout=10,
    )
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        target_host,
        port=target_port,
        username=target_user,
        pkey=private_key,
        sock=channel,
        timeout=10,
    )
    return client


def build_frontend() -> Path:
    if not (FRONTEND_DIR / "node_modules").exists():
        run_local(["npm", "ci"], FRONTEND_DIR)
    env = dict(**os.environ)
    env["VITE_API_BASE_URL"] = ""
    run_local(["npm", "run", "build"], FRONTEND_DIR, env=env)
    dist_dir = FRONTEND_DIR / "dist"
    if not dist_dir.exists():
        raise SystemExit(f"Missing build output: {dist_dir}")
    return dist_dir


def write_runtime_env(path: Path, bind_ip: str, expose_port: int) -> None:
    content = textwrap.dedent(
        f"""\
        FRONTEND_BIND_IP={bind_ip}
        FRONTEND_EXPOSE_PORT={expose_port}
        """
    )
    path.write_text(content, encoding="ascii")


def configure_bastion_forward(
    jump_client: paramiko.SSHClient,
    bastion_port: int,
    target_host: str,
    target_frontend_port: int,
) -> None:
    unit_name = "bridgeability-frontend-port90.service"
    unit_content = textwrap.dedent(
        f"""\
        [Unit]
        Description=BridgeAbility frontend port forward
        After=network.target

        [Service]
        Type=simple
        ExecStart=/usr/bin/ssh -N -g -i /root/.ssh/bridgeability_tunnel -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o StrictHostKeyChecking=no -L 0.0.0.0:{bastion_port}:127.0.0.1:{target_frontend_port} root@{target_host}
        Restart=always
        RestartSec=2

        [Install]
        WantedBy=multi-user.target
        """
    )
    command = textwrap.dedent(
        f"""\
        cat > /etc/systemd/system/{unit_name} <<'EOF'
        {unit_content}EOF
        systemctl daemon-reload
        systemctl enable --now {unit_name}
        systemctl restart {unit_name}
        systemctl status {unit_name} --no-pager
        """
    )
    run_remote(jump_client, command)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jump-host", default=DEFAULT_JUMP_HOST)
    parser.add_argument("--jump-port", type=int, default=22)
    parser.add_argument("--jump-user", default="root")
    parser.add_argument("--jump-private-key-file", type=Path)
    parser.add_argument("--jump-key-path", default="/root/.ssh/bridgeability_tunnel")
    parser.add_argument("--target-host", default=DEFAULT_TARGET_HOST)
    parser.add_argument("--target-port", type=int, default=22)
    parser.add_argument("--target-user", default="root")
    parser.add_argument("--remote-dir", default=DEFAULT_REMOTE_DIR)
    parser.add_argument("--compose-project-name", default=DEFAULT_COMPOSE_PROJECT)
    parser.add_argument("--frontend-bind-ip", default="127.0.0.1")
    parser.add_argument("--frontend-expose-port", type=int, default=DEFAULT_TARGET_PORT)
    parser.add_argument("--bastion-frontend-port", type=int, default=DEFAULT_BASTION_PORT)
    args = parser.parse_args()

    jump_password = os.environ.get("JUMP_PASSWORD")
    jump_private_key_file = args.jump_private_key_file
    env_jump_private_key_file = os.environ.get("JUMP_PRIVATE_KEY_FILE")
    if env_jump_private_key_file:
        jump_private_key_file = Path(env_jump_private_key_file)

    jump_private_key = None
    if jump_private_key_file is not None:
        jump_private_key = load_private_key_from_path(jump_private_key_file)

    if not jump_password and jump_private_key is None:
        raise SystemExit("JUMP_PRIVATE_KEY_FILE or JUMP_PASSWORD is required")

    dist_dir = build_frontend()

    jump = connect_jump(
        args.jump_host,
        args.jump_port,
        args.jump_user,
        password=jump_password if jump_private_key is None else None,
        private_key=jump_private_key,
    )
    try:
        target_key = read_private_key_from_jump(jump, args.jump_key_path)
        target = connect_target(
            jump,
            args.target_host,
            args.target_port,
            args.target_user,
            target_key,
        )
        try:
            with TemporaryDirectory(prefix="erp-frontend-") as temp_dir_raw:
                temp_dir = Path(temp_dir_raw)
                upload_root = temp_dir / "upload"
                upload_root.mkdir(parents=True, exist_ok=True)

                for name in ("Dockerfile", "docker-compose.yml", "nginx.default.conf"):
                    source = FRONTEND_STACK_DIR / name
                    target_path = upload_root / name
                    target_path.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")

                dist_target = upload_root / "dist"
                shutil.copytree(dist_dir, dist_target)

                runtime_env = upload_root / ".env"
                write_runtime_env(runtime_env, args.frontend_bind_ip, args.frontend_expose_port)

                sftp = target.open_sftp()
                try:
                    upload_tree(sftp, upload_root, args.remote_dir)
                finally:
                    sftp.close()

            run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env "
                    f"-f docker-compose.yml up -d --build --force-recreate"
                ),
            )
            output = run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env "
                    f"-f docker-compose.yml ps"
                ),
            )
            print(output.strip())
        finally:
            target.close()

        configure_bastion_forward(
            jump,
            args.bastion_frontend_port,
            args.target_host,
            args.frontend_expose_port,
        )
        print()
        print(f"Frontend URL: http://{args.jump_host}:{args.bastion_frontend_port}")
        print(f"Target frontend bind: {args.frontend_bind_ip}:{args.frontend_expose_port}")
        print(f"Remote stack dir: {args.remote_dir}")
    finally:
        jump.close()


if __name__ == "__main__":
    main()

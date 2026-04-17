import argparse
import os
import secrets
import string
from pathlib import Path

import paramiko
from ssh_jump_auth import (
    connect_jump,
    load_private_key_from_path,
    read_private_key_from_jump,
)


PROJECT_ROOT = Path(__file__).resolve().parent.parent
STACK_DIR = PROJECT_ROOT / "infra" / "remote-middleware"
DEFAULT_ENV_PATH = PROJECT_ROOT / "tmp" / "remote-middleware.generated.env"


def random_secret(length: int) -> str:
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))


def build_default_env() -> dict[str, str]:
    return {
        "MYSQL_BIND_IP": "127.0.0.1",
        "MYSQL_EXPOSE_PORT": "18110",
        "MYSQL_DATABASE": "school_erp",
        "MYSQL_USER": "school_erp",
        "MYSQL_ROOT_PASSWORD": random_secret(24),
        "MYSQL_PASSWORD": random_secret(24),
        "POSTGRES_BIND_IP": "127.0.0.1",
        "POSTGRES_EXPOSE_PORT": "18111",
        "POSTGRES_DB": "school_erp",
        "POSTGRES_USER": "school_erp",
        "POSTGRES_PASSWORD": random_secret(24),
        "REDIS_BIND_IP": "127.0.0.1",
        "REDIS_EXPOSE_PORT": "18112",
        "REDIS_PASSWORD": random_secret(24),
        "RABBITMQ_AMQP_BIND_IP": "127.0.0.1",
        "RABBITMQ_AMQP_PORT": "18113",
        "RABBITMQ_MANAGEMENT_BIND_IP": "127.0.0.1",
        "RABBITMQ_MANAGEMENT_PORT": "18114",
        "RABBITMQ_DEFAULT_USER": "school_erp",
        "RABBITMQ_DEFAULT_PASS": random_secret(24),
        "RABBITMQ_DEFAULT_VHOST": "school-erp",
        "NACOS_HTTP_BIND_IP": "127.0.0.1",
        "NACOS_HTTP_PORT": "18115",
        "NACOS_GRPC_BIND_IP": "127.0.0.1",
        "NACOS_GRPC_PORT": "18116",
        "NACOS_RAFT_BIND_IP": "127.0.0.1",
        "NACOS_RAFT_PORT": "18117",
        "NACOS_DB_NAME": "nacos_config",
        "NACOS_DB_USER": "nacos",
        "NACOS_DB_PASSWORD": random_secret(24),
        "NACOS_AUTH_TOKEN": random_secret(48),
        "NACOS_AUTH_IDENTITY_KEY": "serverIdentity",
        "NACOS_AUTH_IDENTITY_VALUE": random_secret(24),
    }


def ensure_env_file(path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        return path

    env_data = build_default_env()
    lines = [f"{key}={value}" for key, value in env_data.items()]
    path.write_text("\n".join(lines) + "\n", encoding="ascii")
    return path


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


def run_remote(client: paramiko.SSHClient, command: str) -> str:
    stdin, stdout, stderr = client.exec_command(command)
    exit_status = stdout.channel.recv_exit_status()
    output = stdout.read().decode("utf-8", errors="replace")
    error = stderr.read().decode("utf-8", errors="replace")
    if exit_status != 0:
        raise RuntimeError(f"Remote command failed ({exit_status}): {command}\n{output}\n{error}")
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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jump-host", default="8.148.181.9")
    parser.add_argument("--jump-port", type=int, default=22)
    parser.add_argument("--jump-user", default="root")
    parser.add_argument("--jump-private-key-file", type=Path)
    parser.add_argument("--jump-key-path", default="/root/.ssh/bridgeability_tunnel")
    parser.add_argument("--target-host", default="36.137.84.162")
    parser.add_argument("--target-port", type=int, default=22)
    parser.add_argument("--target-user", default="root")
    parser.add_argument("--remote-dir", default="/opt/school-erp/stacks/remote-middleware")
    parser.add_argument("--compose-project-name", default="school-erp-remote-middleware")
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV_PATH)
    parser.add_argument("--reset-volumes", action="store_true")
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

    env_path = ensure_env_file(args.env_file)
    jump = connect_jump(
        args.jump_host,
        args.jump_port,
        args.jump_user,
        password=jump_password if jump_private_key is None else None,
        private_key=jump_private_key,
    )
    try:
        private_key = read_private_key_from_jump(jump, args.jump_key_path)
        target = connect_target(
            jump,
            args.target_host,
            args.target_port,
            args.target_user,
            private_key,
        )
        try:
            sftp = target.open_sftp()
            try:
                upload_tree(sftp, STACK_DIR, args.remote_dir)
                sftp.put(str(env_path), f"{args.remote_dir}/.env")
            finally:
                sftp.close()

            if args.reset_volumes:
                run_remote(
                    target,
                    (
                        f"cd {args.remote_dir} && "
                        f"docker compose -p {args.compose_project_name} --env-file .env -f docker-compose.yml down -v --remove-orphans"
                    ),
                )

            run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env -f docker-compose.yml up -d"
                ),
            )
            output = run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env -f docker-compose.yml ps"
                ),
            )
            print(output.strip())
            print()
            print(f"Environment file: {env_path}")
            print(f"Remote stack dir: {args.remote_dir}")
        finally:
            target.close()
    finally:
        jump.close()


if __name__ == "__main__":
    main()

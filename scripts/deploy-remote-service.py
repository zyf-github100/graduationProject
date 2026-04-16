import argparse
import io
import os
import shlex
from pathlib import Path

import paramiko


PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_DIR = PROJECT_ROOT / "backend"
DEFAULT_COMPOSE_PATH = BACKEND_DIR / "docker-compose.microservice.runtime.yml"
DEFAULT_REMOTE_DIR = "/opt/school-erp/stacks/erp-backend-full"
DEFAULT_REMOTE_ENV_FILE = ".env"
DEFAULT_REMOTE_COMPOSE_FILE = "docker-compose.microservice.runtime.yml"
DEFAULT_COMPOSE_PROJECT = "school-erp-backend-full"
SERVICES = [
    "gateway-service",
    "auth-service",
    "master-service",
    "academic-service",
    "workflow-service",
    "billing-service",
    "notify-service",
]
IMAGE_ENV_BY_SERVICE = {
    service: service.upper().replace("-", "_") + "_IMAGE"
    for service in SERVICES
}
PRIVATE_KEY_TYPES = tuple(
    key_type
    for key_type in (
        getattr(paramiko, "Ed25519Key", None),
        getattr(paramiko, "RSAKey", None),
        getattr(paramiko, "ECDSAKey", None),
        getattr(paramiko, "DSSKey", None),
    )
    if key_type is not None
)


def read_env_text(text: str) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return values


def write_env_text(values: dict[str, str]) -> str:
    return "\n".join(f"{key}={value}" for key, value in values.items()) + "\n"


def default_image_for(service: str, image_repository_prefix: str) -> str:
    return f"{image_repository_prefix}/{service}:latest"


def load_private_key_from_text(key_data: str) -> paramiko.PKey:
    last_error: Exception | None = None
    for key_type in PRIVATE_KEY_TYPES:
        try:
            return key_type.from_private_key(io.StringIO(key_data))
        except paramiko.PasswordRequiredException as exc:
            raise SystemExit(
                "Encrypted SSH private keys are not supported for automated deploys."
            ) from exc
        except paramiko.SSHException as exc:
            last_error = exc
    raise SystemExit("Unsupported SSH private key format.") from last_error


def load_private_key_from_path(path: Path) -> paramiko.PKey:
    return load_private_key_from_text(path.read_text(encoding="utf-8"))


def connect_jump(
    host: str,
    port: int,
    user: str,
    password: str | None = None,
    private_key: paramiko.PKey | None = None,
) -> paramiko.SSHClient:
    if not password and private_key is None:
        raise SystemExit("Jump host authentication is required")

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    connect_kwargs = {
        "hostname": host,
        "port": port,
        "username": user,
        "timeout": 10,
        "allow_agent": False,
        "look_for_keys": False,
    }
    if private_key is not None:
        connect_kwargs["pkey"] = private_key
    else:
        connect_kwargs["password"] = password
    client.connect(**connect_kwargs)
    return client


def read_private_key(jump_client: paramiko.SSHClient, key_path: str) -> paramiko.PKey:
    sftp = jump_client.open_sftp()
    try:
        with sftp.open(key_path, "r") as fp:
            key_data = fp.read().decode("utf-8")
    finally:
        sftp.close()
    return load_private_key_from_text(key_data)


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


def read_remote_file(sftp: paramiko.SFTPClient, remote_path: str) -> str:
    with sftp.open(remote_path, "r") as fp:
        return fp.read().decode("utf-8")


def write_remote_file(sftp: paramiko.SFTPClient, remote_path: str, content: str) -> None:
    parent = remote_path.rsplit("/", 1)[0]
    ensure_remote_dir(sftp, parent)
    with sftp.open(remote_path, "w") as fp:
        fp.write(content)


def update_remote_env(
    sftp: paramiko.SFTPClient,
    remote_dir: str,
    remote_env_file: str,
    service: str,
    image: str,
    image_repository_prefix: str,
) -> None:
    remote_env_path = f"{remote_dir}/{remote_env_file}"
    try:
        current_env_text = read_remote_file(sftp, remote_env_path)
        env_values = read_env_text(current_env_text)
    except FileNotFoundError as exc:
        raise SystemExit(
            f"Missing remote env file: {remote_env_path}. Run the full backend deploy first."
        ) from exc

    for service_name in SERVICES:
        env_key = IMAGE_ENV_BY_SERVICE[service_name]
        env_values.setdefault(
            env_key,
            default_image_for(service_name, image_repository_prefix),
        )
    env_values[IMAGE_ENV_BY_SERVICE[service]] = image
    write_remote_file(sftp, remote_env_path, write_env_text(env_values))


def docker_compose_base(
    remote_dir: str,
    compose_project_name: str,
    remote_env_file: str,
    remote_compose_file: str,
) -> str:
    return (
        f"cd {shlex.quote(remote_dir)} && "
        f"docker compose -p {shlex.quote(compose_project_name)} "
        f"--env-file {shlex.quote(remote_env_file)} "
        f"-f {shlex.quote(remote_compose_file)}"
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--service", choices=SERVICES, required=True)
    parser.add_argument("--image", required=True)
    parser.add_argument("--image-repository-prefix", required=True)
    parser.add_argument("--compose-file", type=Path, default=DEFAULT_COMPOSE_PATH)
    parser.add_argument("--jump-host", default="8.148.181.9")
    parser.add_argument("--jump-port", type=int, default=22)
    parser.add_argument("--jump-user", default="root")
    parser.add_argument("--jump-private-key-file", type=Path)
    parser.add_argument("--jump-key-path", default="/root/.ssh/bridgeability_tunnel")
    parser.add_argument("--target-host", default="36.137.84.162")
    parser.add_argument("--target-port", type=int, default=22)
    parser.add_argument("--target-user", default="root")
    parser.add_argument("--remote-dir", default=DEFAULT_REMOTE_DIR)
    parser.add_argument("--remote-env-file", default=DEFAULT_REMOTE_ENV_FILE)
    parser.add_argument("--remote-compose-file", default=DEFAULT_REMOTE_COMPOSE_FILE)
    parser.add_argument("--compose-project-name", default=DEFAULT_COMPOSE_PROJECT)
    parser.add_argument("--registry", default="ghcr.io")
    parser.add_argument("--registry-username")
    parser.add_argument("--registry-password-env", default="REGISTRY_TOKEN")
    parser.add_argument("--skip-registry-login", action="store_true")
    parser.add_argument("--skip-pull", action="store_true")
    args = parser.parse_args()

    if not args.compose_file.exists():
        raise SystemExit(f"Missing compose file: {args.compose_file}")

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

    if not args.skip_registry_login:
        registry_password = os.environ.get(args.registry_password_env)
        if not args.registry_username or not registry_password:
            raise SystemExit(
                "Registry username and token are required unless --skip-registry-login is set"
            )
    else:
        registry_password = ""

    jump = connect_jump(
        args.jump_host,
        args.jump_port,
        args.jump_user,
        password=jump_password if jump_private_key is None else None,
        private_key=jump_private_key,
    )
    try:
        private_key = read_private_key(jump, args.jump_key_path)
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
                ensure_remote_dir(sftp, args.remote_dir)
                compose_content = args.compose_file.read_text(encoding="utf-8")
                write_remote_file(
                    sftp,
                    f"{args.remote_dir}/{args.remote_compose_file}",
                    compose_content,
                )
                update_remote_env(
                    sftp,
                    args.remote_dir,
                    args.remote_env_file,
                    args.service,
                    args.image,
                    args.image_repository_prefix,
                )
            finally:
                sftp.close()

            compose_base = docker_compose_base(
                args.remote_dir,
                args.compose_project_name,
                args.remote_env_file,
                args.remote_compose_file,
            )

            if not args.skip_registry_login:
                login_command = (
                    f"printf '%s' {shlex.quote(registry_password)} | "
                    f"docker login {shlex.quote(args.registry)} "
                    f"-u {shlex.quote(args.registry_username)} --password-stdin"
                )
                run_remote(target, login_command)

            if not args.skip_pull:
                run_remote(target, f"{compose_base} pull {shlex.quote(args.service)}")

            run_remote(target, f"{compose_base} up -d --no-deps {shlex.quote(args.service)}")
            output = run_remote(target, f"{compose_base} ps {shlex.quote(args.service)}")
            print(output.strip())
        finally:
            target.close()
    finally:
        jump.close()


if __name__ == "__main__":
    main()

import argparse
import io
import os
import subprocess
from pathlib import Path

import paramiko


PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_DIR = PROJECT_ROOT / "backend"
DEFAULT_MIDDLEWARE_ENV_PATH = PROJECT_ROOT / "tmp" / "remote-middleware.generated.env"
DEFAULT_BACKEND_ENV_PATH = PROJECT_ROOT / "tmp" / "backend-microservice.generated.env"
SERVICES = [
    "gateway-service",
    "auth-service",
    "master-service",
    "academic-service",
    "workflow-service",
    "billing-service",
    "notify-service",
]


def read_env_file(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        data[key] = value
    return data


def write_env_file(path: Path, values: dict[str, str]) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = [f"{key}={value}" for key, value in values.items()]
    path.write_text("\n".join(lines) + "\n", encoding="ascii")
    return path


def build_backend_env(args: argparse.Namespace, middleware_env: dict[str, str]) -> dict[str, str]:
    return {
        "GATEWAY_BIND_IP": args.gateway_bind_ip,
        "GATEWAY_EXPOSE_PORT": str(args.gateway_port),
        "CORS_ALLOWED_ORIGIN": args.cors_allowed_origin,
        "NACOS_SERVER_ADDR": "nacos:8848",
        "NACOS_USERNAME": args.nacos_username,
        "NACOS_PASSWORD": args.nacos_password,
        "NACOS_DISCOVERY_ENABLED": "true",
        "NACOS_CONFIG_ENABLED": "false",
        "NACOS_NAMESPACE": "",
        "NACOS_DISCOVERY_GROUP": "DEFAULT_GROUP",
        "NACOS_CONFIG_GROUP": "DEFAULT_GROUP",
        "POSTGRES_HOST": "postgres",
        "POSTGRES_PORT": "5432",
        "POSTGRES_DB": middleware_env.get("POSTGRES_DB", "school_erp"),
        "POSTGRES_USER": middleware_env.get("POSTGRES_USER", "school_erp"),
        "POSTGRES_PASSWORD": middleware_env["POSTGRES_PASSWORD"],
        "REDIS_HOST": "redis",
        "REDIS_PORT": "6379",
        "REDIS_PASSWORD": middleware_env["REDIS_PASSWORD"],
        "RABBITMQ_HOST": "rabbitmq",
        "RABBITMQ_PORT": "5672",
        "RABBITMQ_USERNAME": middleware_env.get("RABBITMQ_DEFAULT_USER", "school_erp"),
        "RABBITMQ_PASSWORD": middleware_env["RABBITMQ_DEFAULT_PASS"],
        "RABBITMQ_VHOST": middleware_env.get("RABBITMQ_DEFAULT_VHOST", "school-erp"),
    }


def run_local(command: str, cwd: Path) -> None:
    completed = subprocess.run(command, cwd=cwd, shell=True, check=False)
    if completed.returncode != 0:
        raise RuntimeError(f"Local command failed ({completed.returncode}): {command}")


def connect_jump(host: str, port: int, user: str, password: str) -> paramiko.SSHClient:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, port=port, username=user, password=password, timeout=10)
    return client


def read_private_key(jump_client: paramiko.SSHClient, key_path: str) -> paramiko.PKey:
    sftp = jump_client.open_sftp()
    try:
        with sftp.open(key_path, "r") as fp:
            key_data = fp.read().decode("utf-8")
    finally:
        sftp.close()
    return paramiko.Ed25519Key.from_private_key(io.StringIO(key_data))


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


def upload_file(sftp: paramiko.SFTPClient, local_path: Path, remote_path: str) -> None:
    parent = remote_path.rsplit("/", 1)[0]
    ensure_remote_dir(sftp, parent)
    sftp.put(str(local_path), remote_path)


def stop_legacy_gateway_if_needed(client: paramiko.SSHClient) -> None:
    command = (
        "if [ -d /opt/school-erp/stacks/remote-gateway ]; then "
        "cd /opt/school-erp/stacks/remote-gateway && "
        "docker compose -p school-erp-remote-gateway --env-file .env -f docker-compose.remote.yml down || true; "
        "fi"
    )
    run_remote(client, command)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jump-host", default="8.148.181.9")
    parser.add_argument("--jump-port", type=int, default=22)
    parser.add_argument("--jump-user", default="root")
    parser.add_argument("--jump-key-path", default="/root/.ssh/bridgeability_tunnel")
    parser.add_argument("--target-host", default="36.137.84.162")
    parser.add_argument("--target-port", type=int, default=22)
    parser.add_argument("--target-user", default="root")
    parser.add_argument("--remote-dir", default="/opt/school-erp/stacks/erp-backend-full")
    parser.add_argument("--compose-project-name", default="school-erp-backend-full")
    parser.add_argument("--middleware-env-file", type=Path, default=DEFAULT_MIDDLEWARE_ENV_PATH)
    parser.add_argument("--backend-env-file", type=Path, default=DEFAULT_BACKEND_ENV_PATH)
    parser.add_argument("--cors-allowed-origin", default="http://localhost:5173")
    parser.add_argument("--gateway-bind-ip", default="0.0.0.0")
    parser.add_argument("--gateway-port", type=int, default=18080)
    parser.add_argument("--nacos-username", default="nacos")
    parser.add_argument("--nacos-password", default="nacos")
    parser.add_argument("--stop-legacy-gateway", action="store_true")
    args = parser.parse_args()

    jump_password = os.environ.get("JUMP_PASSWORD")
    if not jump_password:
        raise SystemExit("JUMP_PASSWORD is required")

    if not args.middleware_env_file.exists():
        raise SystemExit(f"Missing middleware env file: {args.middleware_env_file}")

    middleware_env = read_env_file(args.middleware_env_file)
    backend_env_path = args.backend_env_file
    backend_env = build_backend_env(args, middleware_env)
    write_env_file(backend_env_path, backend_env)

    run_local('mvn clean package -DskipTests "-Dapp.finalName=app"', BACKEND_DIR)

    for service in SERVICES:
        jar_path = BACKEND_DIR / service / "target" / "app.jar"
        if not jar_path.exists():
            raise SystemExit(f"Missing app jar: {jar_path}")

    jump = connect_jump(args.jump_host, args.jump_port, args.jump_user, jump_password)
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
                upload_file(sftp, BACKEND_DIR / ".dockerignore", f"{args.remote_dir}/.dockerignore")
                upload_file(sftp, BACKEND_DIR / "Dockerfile.service", f"{args.remote_dir}/Dockerfile.service")
                upload_file(
                    sftp,
                    BACKEND_DIR / "docker-compose.microservice.yml",
                    f"{args.remote_dir}/docker-compose.microservice.yml",
                )
                upload_file(sftp, backend_env_path, f"{args.remote_dir}/.env")
                for service in SERVICES:
                    jar_path = BACKEND_DIR / service / "target" / "app.jar"
                    upload_file(sftp, jar_path, f"{args.remote_dir}/{service}/target/app.jar")
            finally:
                sftp.close()

            if args.stop_legacy_gateway:
                stop_legacy_gateway_if_needed(target)

            run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env "
                    f"-f docker-compose.microservice.yml up -d --build --force-recreate"
                ),
            )
            output = run_remote(
                target,
                (
                    f"cd {args.remote_dir} && "
                    f"docker compose -p {args.compose_project_name} --env-file .env "
                    f"-f docker-compose.microservice.yml ps"
                ),
            )
            print(output.strip())
            print()
            print(f"Backend env file: {backend_env_path}")
            print(f"Remote stack dir: {args.remote_dir}")
        finally:
            target.close()
    finally:
        jump.close()


if __name__ == "__main__":
    main()

import io
from pathlib import Path

import paramiko


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


def load_private_key_from_text(key_data: str) -> paramiko.PKey:
    last_error: Exception | None = None
    for key_type in PRIVATE_KEY_TYPES:
        try:
            return key_type.from_private_key(io.StringIO(key_data))
        except paramiko.PasswordRequiredException as exc:
            raise SystemExit(
                "Encrypted SSH private keys are not supported for automated SSH flows."
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


def read_private_key_from_jump(
    jump_client: paramiko.SSHClient, key_path: str
) -> paramiko.PKey:
    sftp = jump_client.open_sftp()
    try:
        with sftp.open(key_path, "r") as fp:
            key_data = fp.read().decode("utf-8")
    finally:
        sftp.close()
    return load_private_key_from_text(key_data)

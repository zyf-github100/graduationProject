import argparse
import os
import select
import signal
import socket
import sys
import threading
import time

import paramiko
from pathlib import Path
from ssh_jump_auth import (
    connect_jump,
    load_private_key_from_path,
    read_private_key_from_jump,
)


stop_event = threading.Event()
connections = []


def log(*args):
    print(time.strftime("%Y-%m-%d %H:%M:%S"), *args, flush=True)


def bridge(chan, host, port, origin, remote_port):
    sock = None
    try:
        sock = socket.create_connection((host, port), timeout=10)
        connections.append(sock)
        connections.append(chan)
        log(f"bridge-open remote:{remote_port} origin:{origin} -> local:{host}:{port}")
        while not stop_event.is_set():
            readable, _, _ = select.select([sock, chan], [], [], 1.0)
            if sock in readable:
                data = sock.recv(32768)
                if not data:
                    break
                chan.sendall(data)
            if chan in readable:
                data = chan.recv(32768)
                if not data:
                    break
                sock.sendall(data)
    except Exception as exc:
        log(f"bridge-error remote:{remote_port} local:{host}:{port} err:{exc}")
    finally:
        try:
            if sock:
                sock.close()
        except Exception:
            pass
        try:
            chan.close()
        except Exception:
            pass
        log(f"bridge-close remote:{remote_port} -> local:{host}:{port}")


def handle_signal(signum, frame):
    stop_event.set()


def parse_forward(raw):
    remote_port, local_host, local_port = raw.split(":", 2)
    return int(remote_port), (local_host, int(local_port))


def connect_target(jump_client, target_host, target_port, target_user, private_key):
    chan = jump_client.get_transport().open_channel(
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
        sock=chan,
        timeout=10,
    )
    return client


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jump-host", required=True)
    parser.add_argument("--jump-port", type=int, default=22)
    parser.add_argument("--jump-user", required=True)
    parser.add_argument("--jump-private-key-file", type=Path)
    parser.add_argument("--target-host", required=True)
    parser.add_argument("--target-port", type=int, default=22)
    parser.add_argument("--target-user", required=True)
    parser.add_argument("--jump-key-path", required=True)
    parser.add_argument(
        "--forward",
        action="append",
        required=True,
        help="remotePort:localHost:localPort",
    )
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

    signal.signal(signal.SIGTERM, handle_signal)
    signal.signal(signal.SIGINT, handle_signal)

    forwards = dict(parse_forward(item) for item in args.forward)

    jump = connect_jump(
        args.jump_host,
        args.jump_port,
        args.jump_user,
        password=jump_password if jump_private_key is None else None,
        private_key=jump_private_key,
    )
    log("jump-connected")
    private_key = read_private_key_from_jump(jump, args.jump_key_path)
    target = connect_target(
        jump,
        args.target_host,
        args.target_port,
        args.target_user,
        private_key,
    )
    transport = target.get_transport()
    transport.set_keepalive(30)
    log("target-connected")

    def handler(channel, origin, server_port):
        port_key = server_port[1] if isinstance(server_port, tuple) else server_port
        local_host, local_port = forwards[port_key]
        thread = threading.Thread(
            target=bridge,
            args=(channel, local_host, local_port, origin, server_port),
            daemon=True,
        )
        thread.start()

    for remote_port in forwards:
        transport.request_port_forward("0.0.0.0", remote_port, handler)
        local_host, local_port = forwards[remote_port]
        log(f"reverse-forward 0.0.0.0:{remote_port} -> {local_host}:{local_port}")

    try:
        while not stop_event.is_set() and transport.is_active():
            time.sleep(2)
    finally:
        stop_event.set()
        for conn in connections:
            try:
                conn.close()
            except Exception:
                pass
        try:
            target.close()
        except Exception:
            pass
        try:
            jump.close()
        except Exception:
            pass
        log("tunnel-stopped")


if __name__ == "__main__":
    main()

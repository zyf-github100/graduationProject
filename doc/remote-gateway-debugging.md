# Remote Gateway Joint Debugging

This setup keeps only `gateway-service` on the server and leaves the other backend services on the local machine.
The remote deployment is isolated in its own Docker Compose project and bridge network so it does not interfere with existing services already running on the server.

## Topology

- Remote server:
  - isolated Docker Compose project `school-erp-remote-gateway`
  - isolated bridge network `gateway-edge`
  - `gateway-service` container published on `36.137.84.162:18080`
- Local machine:
  - `auth-service` on `127.0.0.1:8081`
  - `master-service` on `127.0.0.1:8082`
  - `academic-service` on `127.0.0.1:8083`
  - `workflow-service` on `127.0.0.1:8084`
  - `billing-service` on `127.0.0.1:8085`
  - `notify-service` on `127.0.0.1:8086`
  - `frontend` on `127.0.0.1:5173`

The remote gateway container reaches local services through SSH reverse tunnels published on the server host:

- server host `0.0.0.0:18081` -> local `127.0.0.1:8081`
- server host `0.0.0.0:18082` -> local `127.0.0.1:8082`
- server host `0.0.0.0:18083` -> local `127.0.0.1:8083`
- server host `0.0.0.0:18084` -> local `127.0.0.1:8084`
- server host `0.0.0.0:18085` -> local `127.0.0.1:8085`
- server host `0.0.0.0:18086` -> local `127.0.0.1:8086`

Inside the container, these host ports are accessed through `host.docker.internal`.

## One-time deployment

Run on the local machine:

```powershell
.\scripts\deploy-remote-gateway-legacy.ps1
```

If you must go through a bastion host, add jump parameters:

```powershell
.\scripts\deploy-remote-gateway-legacy.ps1 -JumpHost <bastion-ip> -JumpUser <bastion-user> -JumpPort 22 -JumpPrivateKeyFile <local-key-path>
```

This script builds only the `gateway-service` jar locally, uploads `app.jar`, `Dockerfile`, Compose, and `.env`, then builds the image on the server and restarts the isolated remote gateway container.

## Daily joint debugging flow

1. Start all local backend services except `gateway-service`.
2. Open the reverse tunnels and keep the terminal window open:

```powershell
.\scripts\start-remote-gateway-tunnels.ps1
```

If you use a bastion host:

```powershell
.\scripts\start-remote-gateway-tunnels.ps1 -JumpHost <bastion-ip> -JumpUser <bastion-user> -JumpPort 22 -JumpPrivateKeyFile <local-key-path>
```

If the bastion key is already your default local SSH key, for example `~/.ssh/id_rsa`, you can omit `-JumpPrivateKeyFile`.

3. Create `frontend/.env.local` from `frontend/.env.remote-gateway.example` and point the frontend API base to the remote gateway:

```dotenv
VITE_API_BASE_URL=http://36.137.84.162:18080
VITE_DEV_PROXY_TARGET=http://36.137.84.162:18080
```

4. Start the frontend locally:

```powershell
cd frontend
npm run dev
```

5. Access the local frontend at `http://localhost:5173`. Frontend requests go directly to the remote gateway, and the remote gateway calls back into your local services through the SSH tunnels.

## Notes

- The remote deployment contains only `gateway-service`. None of the local backend services are copied to the server as separate containers.
- The uploaded artifacts are limited to `app.jar`, `Dockerfile`, `docker-compose.remote.yml`, and `.env`.
- The gateway container runs in an isolated Docker bridge network and exposes only one external port by default: `18080`.
- For the container to reach the reverse tunnels, the server `sshd` must allow remote forwards to bind on a non-loopback address. In practice, set `GatewayPorts clientspecified` or `GatewayPorts yes` in `/etc/ssh/sshd_config`, then restart `sshd`.
- If `sshd` on the server disables remote forwarding, the tunnel script will fail immediately because `ExitOnForwardFailure=yes` is enabled.
- If port `18080` is already occupied on the server, redeploy with a different value:

```powershell
.\scripts\deploy-remote-gateway-legacy.ps1 -GatewayPort 28080
```

Then update `VITE_DEV_PROXY_TARGET` to the same port.

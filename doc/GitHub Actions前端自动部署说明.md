# GitHub Actions 前端自动部署说明

## 1. 目标

这套自动化部署对应的是：

1. GitHub Actions 在 Runner 上构建前端静态资源
2. 经跳板机把前端部署文件上传到目标机
3. 在目标机执行前端独立 Compose 栈
4. 在跳板机维持 `90 -> 目标机前端 18200` 的持久化 SSH 转发

当前前端部署不是推送到 GHCR，而是：

- Runner 构建 `frontend/dist`
- 目标机使用 `nginx` 容器托管静态资源
- `nginx` 把 `/api` 反向代理到目标机现有网关 `127.0.0.1:18080`

## 2. 仓库内关键文件

- Workflow：`.github/workflows/deploy-frontend.yml`
- 部署脚本：`scripts/deploy-remote-frontend.py`
- PowerShell 包装：`scripts/deploy-remote-frontend.ps1`
- 前端远端栈：`infra/remote-frontend/docker-compose.yml`
- Nginx 配置：`infra/remote-frontend/nginx.default.conf`

## 3. 触发规则

### 3.1 Push 自动触发

当 `main` 分支发生以下路径变更时，Workflow 会自动执行：

- `frontend/**`
- `infra/remote-frontend/**`
- `scripts/deploy-remote-frontend.py`
- `scripts/ssh_jump_auth.py`
- `.github/workflows/deploy-frontend.yml`

### 3.2 手动触发

支持 `workflow_dispatch` 手动执行整套前端部署。

## 4. GitHub Secrets

### 4.1 必填项

- `BASTION_HOST`
- `BASTION_PORT`
- `BASTION_USER`
- `BASTION_SSH_PRIVATE_KEY`
- `TARGET_HOST`
- `TARGET_PORT`
- `TARGET_USER`

说明：

- `BASTION_SSH_PRIVATE_KEY` 是 GitHub Actions Runner 登录跳板机使用的私钥全文。
- 目标机登录仍然依赖跳板机本地已有的目标机私钥文件。

### 4.2 推荐配置

- `TARGET_SSH_KEY_PATH`
  - 含义：跳板机上用于登录目标机的私钥路径
  - 默认值：`/root/.ssh/bridgeability_tunnel`
- `FRONTEND_REMOTE_DIR`
  - 默认值：`/opt/school-erp/stacks/erp-frontend`
- `FRONTEND_COMPOSE_PROJECT`
  - 默认值：`school-erp-frontend`
- `FRONTEND_BIND_IP`
  - 默认值：`127.0.0.1`
- `FRONTEND_EXPOSE_PORT`
  - 默认值：`18200`
- `BASTION_FRONTEND_PORT`
  - 默认值：`90`

## 5. 实际执行过程

Workflow 做的是：

1. 安装 Node.js 和 Python
2. 在 Runner 上执行 `frontend` 的生产构建
3. 把 `infra/remote-frontend` 和 `frontend/dist` 上传到目标机前端栈目录
4. 在目标机执行：

```bash
docker compose -p school-erp-frontend --env-file .env -f docker-compose.yml up -d --build --force-recreate
```

5. 在跳板机写入并重启：

```text
bridgeability-frontend-port90.service
```

它实际维持的是：

```text
0.0.0.0:90 -> 目标机 127.0.0.1:18200
```

## 6. 当前限制

Workflow 只负责把前端和跳板机转发服务部署好。

如果浏览器无法访问 `http://<bastion-ip>:90/`，常见原因是跳板机云侧安全组没有放行 TCP `90`。这一步不在仓库自动化内，需要在云控制台单独配置。

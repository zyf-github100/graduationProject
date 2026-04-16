# GitHub Actions 微服务自动部署说明

## 1. 目标

这套自动化部署对应的是：

1. GitHub Actions 按微服务构建镜像
2. 镜像推送到 `GHCR`
3. Actions 使用跳板机私钥连接跳板机
4. 再由跳板机使用已有目标机私钥登录目标服务器
5. 目标机只拉取并重启发生变更的那个微服务

它不是“整套后端每次一起重发”，而是“单个微服务独立发布”。

## 2. 仓库内关键文件

- Workflow：`.github/workflows/deploy-backend-services.yml`
- 变更识别脚本：`scripts/select-backend-services.py`
- 单服务远端部署脚本：`scripts/deploy-remote-service.py`
- 运行时 Compose：`backend/docker-compose.microservice.runtime.yml`

## 3. 触发规则

### 3.1 Push 自动触发

当 `main` 分支发生以下路径变更时，Workflow 会自动执行：

- `backend/**`
- `scripts/deploy-remote-service.py`
- `scripts/select-backend-services.py`
- `.github/workflows/deploy-backend-services.yml`

### 3.2 服务选择规则

Workflow 会先根据改动范围决定要发布哪些服务：

1. 如果改动的是某个服务目录，例如 `backend/auth-service/**`，则只发布该服务。
2. 如果改动的是公共层，例如：
   - `backend/erp-common/**`
   - `backend/pom.xml`
   - `backend/Dockerfile.service`
   - `backend/.dockerignore`
   - `backend/docker-compose.microservice.runtime.yml`
3. 公共层改动会触发全部 7 个服务重新发布。

### 3.3 手动触发

`workflow_dispatch` 支持手动指定：

- `auto`
- `all`
- `gateway-service`
- `auth-service`
- `master-service`
- `academic-service`
- `workflow-service`
- `billing-service`
- `notify-service`

## 4. 镜像命名规则

每个服务单独生成镜像，推送到：

```text
ghcr.io/<github-owner>/school-erp/<service>:sha-<commit-sha>
```

在 `main` 分支上还会额外推送：

```text
ghcr.io/<github-owner>/school-erp/<service>:latest
```

例如：

```text
ghcr.io/zyf-github100/school-erp/gateway-service:sha-abc123
ghcr.io/zyf-github100/school-erp/gateway-service:latest
```

## 5. 服务器运行方式

目标机使用的是运行时 Compose：

```text
backend/docker-compose.microservice.runtime.yml
```

这份 Compose 不在服务器本地构建镜像，只负责：

- 从 `.env` 读取中间件连接配置
- 从 `.env` 读取每个微服务对应的镜像地址
- 拉取镜像并重建目标服务

自动化部署只负责更新应用服务镜像，不会动已经独立运行的：

- PostgreSQL
- Redis
- RabbitMQ
- Nacos

## 6. 首次启用前必须完成的事

这套 GitHub Actions 不是“零初始化”。

首次启用前，目标机上必须已经有：

1. 完整后端部署目录：`/opt/school-erp/stacks/erp-backend-full`
2. 后端运行环境文件：`.env`
3. 中间件网络：`school-erp-middleware-net`
4. Docker / Docker Compose 可正常使用

这一步仍然通过当前全量部署脚本完成：

```powershell
$env:JUMP_PASSWORD='...'
.\scripts\deploy-remote-backend-full.ps1 -StopLegacyGateway
```

自动化部署是在这之后接管“单个微服务更新”。

## 7. GitHub Secrets

### 7.1 跳板机

- `BASTION_HOST`
- `BASTION_PORT`
- `BASTION_USER`
- `BASTION_SSH_PRIVATE_KEY`

说明：

- `BASTION_SSH_PRIVATE_KEY` 存的是 GitHub Actions Runner 用来登录跳板机的私钥全文。
- 需要和跳板机 `/root/.ssh/authorized_keys` 中已写入的公钥配对。
- 当前跳板机已经关闭 SSH 密码登录，因此这里不能再用 `BASTION_PASSWORD`。

### 7.2 目标机

- `TARGET_HOST`
- `TARGET_PORT`
- `TARGET_USER`

### 7.3 镜像仓库

- `GHCR_TOKEN`

说明：

- Workflow 构建和推送镜像使用的是 Actions 自带的 `GITHUB_TOKEN`。
- `GHCR_TOKEN` 只在目标机拉取私有 GHCR 镜像时用于 `docker login ghcr.io`。
- 如果对应 GHCR 包允许匿名拉取，可以先不配。
- 如果需要配置，建议至少具备 `read:packages`。

### 7.4 可选项

- `TARGET_SSH_KEY_PATH`
  - 含义：跳板机上用于登录目标机的私钥路径
  - 默认值：`/root/.ssh/bridgeability_tunnel`
- `BACKEND_REMOTE_DIR`
  - 默认值：`/opt/school-erp/stacks/erp-backend-full`
- `BACKEND_COMPOSE_PROJECT`
  - 默认值：`school-erp-backend-full`
- `BACKEND_REMOTE_ENV_FILE`
  - 默认值：`.env`
- `BACKEND_REMOTE_COMPOSE_FILE`
  - 默认值：`docker-compose.microservice.runtime.yml`

## 8. Actions 实际执行过程

以 `auth-service` 为例，Actions 做的是：

1. Runner 本地执行：

```bash
cd backend
mvn -pl auth-service -am clean package -DskipTests "-Dapp.finalName=app"
```

2. 构建并推送镜像：

```text
ghcr.io/<owner>/school-erp/auth-service:sha-<commit>
```

3. 将 `BASTION_SSH_PRIVATE_KEY` 写入 Runner 临时文件
4. 使用该私钥连接跳板机
5. 再由跳板机使用 `/root/.ssh/bridgeability_tunnel` 登录目标机
6. 上传运行时 Compose
7. 更新目标机 `.env` 中的：

```text
AUTH_SERVICE_IMAGE=ghcr.io/<owner>/school-erp/auth-service:sha-<commit>
```

8. 目标机执行：

```bash
docker compose pull auth-service
docker compose up -d --no-deps auth-service
```

这样只会更新 `auth-service`，不会把其他服务一起重建。

## 9. 推荐运维边界

建议保持下面这个边界不变：

- GitHub Actions 管应用服务
- 中间件继续由独立 Compose 栈管理

不要把下面这些也混进每次业务发布：

- MySQL / PostgreSQL
- Redis
- RabbitMQ
- Nacos

否则回滚、故障隔离和定位都会变复杂。

## 10. 当前限制

这套自动化已经适合“微服务逐个发布”，但还没补这些能力：

1. 自动健康检查失败回滚
2. 多环境区分，例如 `dev / test / prod`
3. 前端自动部署
4. Nacos 配置自动下发
5. 数据库迁移版本化

所以当前阶段，它是“可用的持续部署基础版”，不是完整生产发布平台。

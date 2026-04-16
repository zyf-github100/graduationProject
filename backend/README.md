# School ERP Backend

后端按项目文档中的技术栈搭建：

- Java 17
- Spring Boot
- Spring Cloud Alibaba
- Spring Cloud Gateway
- Spring Security + JWT
- MyBatis-Plus
- PostgreSQL
- Redis
- RabbitMQ
- MinIO
- Maven

## 模块说明

- `gateway-service`：统一入口与路由转发
- `auth-service`：认证、会话、菜单、权限
- `master-service`：基础数据，当前先落学生档案接口
- `academic-service`：教务概览接口
- `workflow-service`：审批待办与流转接口
- `billing-service`：收费概览与账单接口
- `notify-service`：消息通知占位服务
- `erp-common`：统一响应、异常、请求链路等公共能力

## 构建

```bash
cd backend
mvn clean package
```

## 运行模式

后端现在有两种明确的运行方式：

- `standalone`：默认模式，保留当前演示数据和本地直连方式，不依赖 Nacos/RabbitMQ。
- `microservice`：微服务模式，服务注册到 Nacos，网关通过服务发现转发，`workflow-service` 会通过 RabbitMQ 异步通知 `notify-service`。

## 本地 standalone 启动顺序

1. `auth-service`
2. `master-service`
3. `academic-service`
4. `workflow-service`
5. `billing-service`
6. `notify-service`
7. `gateway-service`

每个服务都可通过：

```bash
mvn -pl auth-service spring-boot:run
```

把模块名替换为对应服务即可。

## Microservice 模式

1. 先确保远端或本机已有 Nacos / PostgreSQL / Redis / RabbitMQ，并且后端容器或进程可访问它们。
2. 在 `backend/.env.microservice.example` 基础上准备一份 `.env.microservice`。
3. 先执行 `mvn package -DskipTests "-Dapp.finalName=app"` 生成各模块 jar。
4. 使用 Docker Compose 启动整套后端：

```bash
cd backend
docker compose --env-file .env.microservice -f docker-compose.microservice.yml up -d --build
```

在该模式下：

- 网关使用 `lb://service-name` 路由，不再硬编码 `localhost`。
- `academic-service` 会通过 Feign 调用 `master-service` 的内部接口。
- `workflow-service` 会把审批状态事件发送到 RabbitMQ。
- `notify-service` 会消费这些事件，并可通过 `/api/v1/notify/tasks/inbox` 查看收件箱。

## 默认端口

- `gateway-service`: `8080`
- `auth-service`: `8081`
- `master-service`: `8082`
- `academic-service`: `8083`
- `workflow-service`: `8084`
- `billing-service`: `8085`
- `notify-service`: `8086`

## 演示账号

- 用户名：`admin.wang`
- 密码：`123456`
- 客户端：`ADMIN_WEB`

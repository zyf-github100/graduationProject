# 学校 ERP 系统

## 接口设计规范说明书

**项目名称：** 学校 ERP 系统一期建设项目  
**文档名称：** 接口设计规范说明书  
**版本号：** V1.0  
**编写日期：** 2026-04-16  
**文档状态：** 评审版  

------

# 1 引言

## 1.1 编写目的

本文档用于统一学校 ERP 一期项目的接口设计规范，包括前后端 HTTP API、服务间同步接口、异步事件、外部系统回调、文件接口、错误码、幂等、鉴权和文档治理要求，确保所有服务在协作时具备统一风格和稳定契约。

## 1.2 适用范围

本文档适用于：

1. 管理后台调用的 API；
2. 教师端调用的 API；
3. 网关到各业务服务的 HTTP API；
4. 服务间同步查询和提交接口；
5. RabbitMQ 异步消息事件；
6. 支付、短信、企业微信/钉钉等第三方对接接口。

## 1.3 关联文档

1. 《系统总体架构设计说明书》
2. 《微服务拆分与服务边界清单》
3. 《数据库总体设计说明书》

------

# 2 总体原则

## 2.1 统一入口原则

所有前端与外部请求必须统一通过 API 网关，不允许客户端直接访问内部服务。

## 2.2 契约先行原则

接口开发前必须先定义：

1. URL 与 HTTP 方法；
2. 请求参数和响应结构；
3. 权限要求；
4. 错误码；
5. 幂等要求；
6. 审计要求；
7. 是否触发后续事件。

## 2.3 向后兼容原则

1. 已发布接口不得随意修改字段含义；
2. 新增字段应保证旧客户端兼容；
3. 重大不兼容变更需要升级 API 版本；
4. 老版本下线前必须有迁移窗口。

## 2.4 安全优先原则

接口设计必须考虑：

1. 身份认证；
2. 功能权限；
3. 数据权限；
4. 参数校验；
5. 幂等与防重；
6. 审计与追踪。

------

# 3 协议与基础约定

## 3.1 协议

1. 统一采用 HTTP/HTTPS；
2. 生产环境必须使用 HTTPS；
3. 默认 `Content-Type` 为 `application/json;charset=UTF-8`；
4. 文件上传使用 `multipart/form-data`；
5. 文件下载使用流式响应。

## 3.2 版本规范

统一使用路径版本号：

```text
/api/v1/...
```

规则如下：

1. 向后兼容的新增字段不升大版本；
2. 不兼容修改才升级 `v2`；
3. 新老版本切换应可灰度。

## 3.3 服务路由前缀

| 服务 | 路由前缀 |
| --- | --- |
| 认证权限服务 | `/api/v1/auth` |
| 基础数据服务 | `/api/v1/master` |
| 教务服务 | `/api/v1/academic` |
| 审批服务 | `/api/v1/workflow` |
| 收费服务 | `/api/v1/billing` |
| 消息通知服务 | `/api/v1/notify` |

------

# 4 URL 与 HTTP 方法规范

## 4.1 URL 命名规范

1. 使用名词复数表示资源；
2. 使用小写字母和短横线；
3. 不在 URL 中使用驼峰；
4. 尽量不用动词；
5. 动作类接口采用 `/{id}/action` 形式。

推荐示例：

```text
GET    /api/v1/master/students
GET    /api/v1/master/students/{studentId}
POST   /api/v1/workflow/leave-requests
POST   /api/v1/workflow/leave-requests/{id}/approve
POST   /api/v1/billing/bills/generate
```

不推荐示例：

```text
GET    /api/v1/getStudentList
POST   /api/v1/doApprove
POST   /api/v1/updateBillStatus
```

## 4.2 HTTP 方法规范

| 方法 | 场景 | 说明 |
| --- | --- | --- |
| `GET` | 查询 | 幂等、只读 |
| `POST` | 创建资源或触发动作 | 非幂等，需要结合幂等控制 |
| `PUT` | 全量更新 | 幂等 |
| `PATCH` | 局部更新 | 优先用于状态或局部字段更新 |
| `DELETE` | 删除资源 | 优先逻辑删除 |

------

# 5 请求头与认证规范

## 5.1 标准请求头

| Header | 是否必填 | 说明 |
| --- | --- | --- |
| `Authorization` | 是 | `Bearer {token}` |
| `Content-Type` | 是 | JSON 或 multipart |
| `X-Request-Id` | 建议 | 全链路请求 ID |
| `X-Client-Type` | 建议 | `ADMIN_WEB`、`TEACHER_H5` 等 |
| `X-Idempotency-Key` | 场景必填 | 防重场景使用 |

## 5.2 认证规则

1. 网关先做令牌合法性校验；
2. 下游服务根据用户上下文做功能权限和数据权限校验；
3. 前端菜单权限仅作为展示控制，最终以后端校验为准；
4. 高风险接口必须记录审计日志。

## 5.3 数据权限规则

数据权限至少支持以下维度：

1. 按校区；
2. 按年级；
3. 按班级；
4. 按教师本人；
5. 按申请人本人；
6. 按财务岗位或组织单元。

------

# 6 请求参数与字段规范

## 6.1 字段命名

1. JSON 字段统一使用 `camelCase`；
2. 枚举值统一使用大写下划线；
3. 布尔字段使用 `is`、`has`、`can` 前缀；
4. 时间字段使用 `At` 或 `Time` 后缀；
5. 日期字段使用 `Date` 后缀。

## 6.2 分页参数

分页查询统一使用：

| 参数 | 说明 |
| --- | --- |
| `pageNo` | 页码，从 1 开始 |
| `pageSize` | 每页条数 |
| `keyword` | 关键字 |
| `sortBy` | 排序字段 |
| `sortOrder` | `asc` 或 `desc` |

约束：

1. `pageSize` 必须有最大值限制；
2. 排序字段必须白名单校验；
3. 时间范围优先使用 `startDate/endDate` 或 `startTime/endTime`。

## 6.3 批量操作

批量操作统一采用数组参数：

```json
{
  "ids": [1001, 1002, 1003]
}
```

对于大批量导入导出：

1. 创建异步任务；
2. 返回任务 ID；
3. 轮询查询执行状态或通过消息告知结果。

------

# 7 响应体规范

## 7.1 标准响应结构

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "requestId": "20260416-abc123",
  "timestamp": "2026-04-16T16:00:00+08:00"
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `code` | 业务状态码 |
| `message` | 说明信息 |
| `data` | 业务数据 |
| `requestId` | 请求唯一标识 |
| `timestamp` | 服务响应时间 |

## 7.2 分页响应结构

```json
{
  "code": "SUCCESS",
  "message": "查询成功",
  "data": {
    "records": [],
    "pageNo": 1,
    "pageSize": 20,
    "total": 125,
    "totalPages": 7
  },
  "requestId": "20260416-abc123",
  "timestamp": "2026-04-16T16:00:00+08:00"
}
```

## 7.3 响应约束

1. 列表无数据时返回空数组，不返回 `null`；
2. 单对象不存在时返回业务错误码；
3. 文件下载失败时回退为标准 JSON 错误体；
4. 响应中不得泄露内部异常堆栈给前端。

------

# 8 错误码规范

## 8.1 HTTP 状态码建议

| 状态码 | 场景 |
| --- | --- |
| `200` | 成功 |
| `400` | 参数错误 |
| `401` | 未登录或令牌失效 |
| `403` | 无权限 |
| `404` | 资源不存在 |
| `409` | 状态冲突、重复提交 |
| `422` | 业务规则校验失败 |
| `429` | 请求过于频繁 |
| `500` | 系统内部错误 |
| `503` | 服务暂不可用 |

## 8.2 业务错误码建议

| 业务码 | 说明 |
| --- | --- |
| `SUCCESS` | 成功 |
| `PARAM_INVALID` | 参数不合法 |
| `UNAUTHORIZED` | 未登录或令牌无效 |
| `FORBIDDEN` | 功能权限不足 |
| `DATA_SCOPE_FORBIDDEN` | 数据范围不足 |
| `RESOURCE_NOT_FOUND` | 资源不存在 |
| `RESOURCE_CONFLICT` | 资源冲突 |
| `DUPLICATE_SUBMIT` | 重复提交 |
| `STATUS_INVALID` | 当前状态不允许该操作 |
| `BIZ_RULE_VIOLATION` | 业务规则不满足 |
| `IDEMPOTENCY_CONFLICT` | 幂等冲突 |
| `THIRD_PARTY_ERROR` | 第三方调用失败 |
| `SYSTEM_ERROR` | 系统内部错误 |

## 8.3 错误响应示例

```json
{
  "code": "DATA_SCOPE_FORBIDDEN",
  "message": "当前用户无权查看该班级数据",
  "data": null,
  "requestId": "20260416-err-001",
  "timestamp": "2026-04-16T16:10:00+08:00"
}
```

------

# 9 幂等与并发控制

## 9.1 必须幂等的接口

以下接口必须保证幂等：

1. 提交审批；
2. 生成账单；
3. 支付回调；
4. 退费确认；
5. 导入任务提交；
6. 消息发送请求；
7. 成绩发布；
8. 批量调课确认。

## 9.2 幂等实现建议

1. 优先使用 `X-Idempotency-Key`；
2. 配合业务唯一键，例如 `billNo`、`processNo`；
3. 使用数据库唯一约束防止重复落库；
4. 对支付回调和事件消费使用幂等记录表或 Redis 键。

## 9.3 并发控制建议

1. 更新类操作使用乐观锁版本号；
2. 状态流转接口必须校验当前状态；
3. 支付、退费等高冲突接口需要防重复处理；
4. 导入任务和消息任务应避免并发创建重复记录。

------

# 10 异步事件规范

## 10.1 适用场景

异步事件主要用于：

1. 审批结果驱动教务或收费更新；
2. 账单生成和支付成功后的通知；
3. 成绩发布后的通知；
4. 报表读库同步；
5. 审计日志异步处理。

## 10.2 事件命名

事件名统一采用业务事实过去式：

1. `LeaveApproved`
2. `ScheduleAdjusted`
3. `BillGenerated`
4. `PaymentCompleted`
5. `GradePublished`
6. `MasterDataChanged`

## 10.3 事件消息结构

```json
{
  "eventId": "evt-20260416-0001",
  "eventType": "LeaveApproved",
  "source": "workflow-service",
  "occurredAt": "2026-04-16T16:20:00+08:00",
  "bizId": 900001,
  "bizNo": "LEAVE202604160001",
  "operatorId": 10001,
  "idempotencyKey": "workflow:leave:900001:approved",
  "data": {
    "studentId": 20001,
    "leaveDate": "2026-04-18",
    "leaveSection": "AM"
  }
}
```

## 10.4 事件设计要求

1. 事件必须表示已发生事实；
2. 事件体必须包含业务主键和幂等键；
3. 消费方必须幂等；
4. 失败必须支持重试和死信；
5. 事件版本升级要兼容老消费者。

------

# 11 文件与第三方接口规范

## 11.1 文件上传下载

上传接口要求：

1. 使用 `multipart/form-data`；
2. 限制文件类型和大小；
3. 返回文件 ID、对象键或访问令牌；
4. 上传行为记录审计日志。

下载接口要求：

1. 先做权限校验；
2. 可返回短时签名地址；
3. 下载行为必须记录；
4. 文件不存在时返回标准错误码。

## 11.2 外部系统集成

一期重点外部对接包括：

1. 短信平台；
2. 企业微信或钉钉；
3. 支付接口或银校接口；
4. 历史数据导入接口；
5. 设备或考勤数据接口。

设计要求：

1. 业务服务不直接耦合第三方 SDK；
2. 第三方差异由适配层屏蔽；
3. 外部接口错误必须标准化；
4. 对接失败必须有重试或补偿机制。

## 11.3 外部回调规范

1. 回调接口必须校验来源签名或白名单；
2. 必须幂等；
3. 必须记录原始报文；
4. 必须区分“验签失败、重复回调、业务失败”；
5. 不得因为重复回调导致重复记账。

------

# 12 文档化与测试要求

## 12.1 OpenAPI 文档要求

所有 HTTP API 应维护 OpenAPI 文档，至少包含：

1. 接口用途；
2. 权限要求；
3. 请求参数；
4. 响应结构；
5. 错误码；
6. 示例报文；
7. 幂等说明。

## 12.2 Mock 与联调要求

1. 接口开发前先出契约；
2. 联调前提供 Mock 数据；
3. 核心接口至少提供成功、失败、权限不足三类样例；
4. 核心事件必须提供样例消息体和消费样例。

## 12.3 测试重点

接口测试至少覆盖：

1. 参数合法性；
2. 功能权限；
3. 数据范围权限；
4. 幂等；
5. 错误码；
6. 并发；
7. 审计日志。

------

# 13 核心接口清单建议

## 13.1 认证权限服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/auth/login` | 登录 |
| `POST` | `/api/v1/auth/logout` | 登出 |
| `POST` | `/api/v1/auth/token/refresh` | 刷新令牌 |
| `GET` | `/api/v1/auth/me` | 当前用户信息 |
| `GET` | `/api/v1/auth/me/menus` | 当前菜单 |
| `GET` | `/api/v1/auth/me/permissions` | 当前权限 |

## 13.2 基础数据服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/master/students` | 学生列表 |
| `GET` | `/api/v1/master/students/{id}` | 学生详情 |
| `POST` | `/api/v1/master/students/import-tasks` | 学生导入 |
| `GET` | `/api/v1/master/classes` | 班级列表 |
| `GET` | `/api/v1/master/courses` | 课程列表 |

## 13.3 教务服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `GET` | `/api/v1/academic/timetables` | 课表列表 |
| `POST` | `/api/v1/academic/timetables/generate` | 生成课表 |
| `GET` | `/api/v1/academic/attendance-records` | 考勤查询 |
| `POST` | `/api/v1/academic/attendance-records` | 提交考勤 |
| `POST` | `/api/v1/academic/grade-tasks/{id}/publish` | 发布成绩 |

## 13.4 审批服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/workflow/leave-requests` | 提交请假申请 |
| `POST` | `/api/v1/workflow/leave-requests/{id}/approve` | 请假审批通过 |
| `POST` | `/api/v1/workflow/leave-requests/{id}/reject` | 请假审批驳回 |
| `POST` | `/api/v1/workflow/schedule-change-requests` | 提交调课申请 |
| `GET` | `/api/v1/workflow/tasks/todo` | 待办列表 |

## 13.5 收费服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/billing/fee-items` | 创建费用项目 |
| `POST` | `/api/v1/billing/bills/generate` | 生成账单 |
| `GET` | `/api/v1/billing/bills` | 账单列表 |
| `POST` | `/api/v1/billing/receipts` | 收款登记 |
| `POST` | `/api/v1/billing/payment/callback` | 支付回调 |

## 13.6 消息通知服务

| 方法 | 接口 | 说明 |
| --- | --- | --- |
| `POST` | `/api/v1/notify/messages/send` | 创建消息任务 |
| `GET` | `/api/v1/notify/messages/{taskId}` | 查询消息任务 |
| `POST` | `/api/v1/notify/templates` | 创建模板 |
| `GET` | `/api/v1/notify/templates` | 模板列表 |

------

# 14 典型报文示例

## 14.1 登录接口

### 请求

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "******",
  "clientType": "ADMIN_WEB"
}
```

### 响应

```json
{
  "code": "SUCCESS",
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "expiresIn": 7200
  },
  "requestId": "20260416-login-001",
  "timestamp": "2026-04-16T16:30:00+08:00"
}
```

## 14.2 提交请假申请

### 请求

```http
POST /api/v1/workflow/leave-requests
Authorization: Bearer xxx
X-Idempotency-Key: leave-apply-20260416-001
Content-Type: application/json

{
  "studentId": 20001,
  "leaveType": "SICK",
  "startDate": "2026-04-18",
  "endDate": "2026-04-18",
  "leaveSection": "AM",
  "reason": "发烧就医"
}
```

### 响应

```json
{
  "code": "SUCCESS",
  "message": "提交成功",
  "data": {
    "processId": 600001,
    "processNo": "LEAVE202604160001",
    "status": "SUBMITTED"
  },
  "requestId": "20260416-leave-001",
  "timestamp": "2026-04-16T16:35:00+08:00"
}
```

## 14.3 账单生成

### 请求

```http
POST /api/v1/billing/bills/generate
Authorization: Bearer xxx
X-Idempotency-Key: bill-generate-20260416-term1
Content-Type: application/json

{
  "termId": 50001,
  "feeItemId": 70001,
  "classIds": [30001, 30002]
}
```

### 响应

```json
{
  "code": "SUCCESS",
  "message": "账单生成任务已提交",
  "data": {
    "taskId": 800001,
    "taskStatus": "PROCESSING"
  },
  "requestId": "20260416-bill-001",
  "timestamp": "2026-04-16T16:40:00+08:00"
}
```

------

# 15 结论

本接口规范以“统一入口、统一鉴权、统一响应、统一错误码、统一幂等、统一事件语义”为核心目标，确保学校 ERP 一期在多服务协作下仍然能够保持接口风格一致、联调成本可控、问题可追踪、扩展可持续。后续所有新增 API、事件和第三方集成都应以本文档为统一基线执行。

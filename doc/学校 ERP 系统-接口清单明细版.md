# 学校 ERP 系统

## 接口清单明细版

**项目名称：** 学校 ERP 系统一期建设项目  
**文档名称：** 接口清单明细版  
**版本号：** V1.0  
**编写日期：** 2026-04-16  
**文档状态：** 设计版  

------

# 1 引言

## 1.1 编写目的

本文档在《接口设计规范说明书》的基础上，进一步把学校 ERP 一期项目的核心接口细化到“接口清单级别”。每个接口均明确其所属服务、接口路径、调用方式、业务用途、权限要求、请求参数、响应字段、幂等要求、触发事件和实现备注，用于支撑：

1. 前后端联调；
2. 服务间联调；
3. 接口开发分工；
4. 测试用例设计；
5. OpenAPI 文档录入；
6. 项目评审和交付材料归档。

## 1.2 适用范围

本文档覆盖以下服务的核心接口：

1. 认证权限服务；
2. 基础数据服务；
3. 教务服务；
4. 审批服务；
5. 收费服务；
6. 消息通知服务。

## 1.3 说明

1. 本文档优先列出一期必须落地的核心接口；
2. 接口参数为建议结构，可在不破坏统一规范的前提下补充；
3. 所有接口默认由 API 网关统一接入；
4. 这里列出的响应字段聚焦主要业务字段，不等于最终 OpenAPI 的全部字段。

------

# 2 接口清单总览

## 2.1 服务接口数量建议

| 服务 | 核心接口数 | 说明 |
| --- | --- | --- |
| 认证权限服务 | 6 | 登录、令牌、用户信息、菜单、权限 |
| 基础数据服务 | 7 | 学生、教师、班级、课程、导入 |
| 教务服务 | 7 | 课表、考勤、成绩 |
| 审批服务 | 7 | 请假、调课、待办、流程详情 |
| 收费服务 | 7 | 费用项目、账单、收款、回调 |
| 消息通知服务 | 5 | 模板、消息任务、发送明细 |

## 2.2 统一响应约定

所有接口均遵循以下响应骨架：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "requestId": "20260416-demo-001",
  "timestamp": "2026-04-16T17:00:00+08:00"
}
```

------

# 3 认证权限服务接口明细

## 3.1 `POST /api/v1/auth/login`

### 3.1.1 接口用途

用户登录，完成账号密码校验并返回令牌。

### 3.1.2 权限要求

公共接口，无需登录，但受验证码、失败锁定和限流控制。

### 3.1.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `username` | string | 是 | 登录账号 |
| `password` | string | 是 | 登录密码 |
| `clientType` | string | 是 | 客户端类型，如 `ADMIN_WEB` |
| `captchaCode` | string | 否 | 验证码 |
| `captchaKey` | string | 否 | 验证码键 |

### 3.1.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | string | 访问令牌 |
| `refreshToken` | string | 刷新令牌 |
| `expiresIn` | integer | 过期秒数 |
| `userId` | bigint | 用户 ID |
| `displayName` | string | 显示名称 |
| `userType` | string | 用户类型 |

### 3.1.5 特殊规则

1. 登录失败次数过多应触发锁定；
2. 登录成功后写登录日志；
3. 必须生成会话记录；
4. 同一账号是否允许多端并发登录由配置控制。

## 3.2 `POST /api/v1/auth/logout`

### 3.2.1 接口用途

当前用户主动登出。

### 3.2.2 权限要求

需登录。

### 3.2.3 请求参数

无业务请求体，依赖当前令牌。

### 3.2.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `success` | boolean | 是否成功登出 |

### 3.2.5 特殊规则

1. 会话状态更新为 `LOGGED_OUT`；
2. Redis 会话缓存应失效；
3. 需记录登出日志。

## 3.3 `POST /api/v1/auth/token/refresh`

### 3.3.1 接口用途

刷新令牌。

### 3.3.2 权限要求

公共接口，但必须提交合法的刷新令牌。

### 3.3.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `refreshToken` | string | 是 | 刷新令牌 |

### 3.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `accessToken` | string | 新访问令牌 |
| `refreshToken` | string | 新刷新令牌 |
| `expiresIn` | integer | 过期秒数 |

### 3.3.5 特殊规则

1. 旧刷新令牌失效策略需要明确；
2. 若会话已失效，应返回 `UNAUTHORIZED`。

## 3.4 `GET /api/v1/auth/me`

### 3.4.1 接口用途

获取当前登录用户基础信息。

### 3.4.2 权限要求

需登录。

### 3.4.3 查询参数

无。

### 3.4.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | bigint | 用户 ID |
| `username` | string | 登录账号 |
| `displayName` | string | 显示名称 |
| `userType` | string | 用户类型 |
| `roles` | array | 角色集合 |
| `orgUnitId` | bigint | 组织单元 |

## 3.5 `GET /api/v1/auth/me/menus`

### 3.5.1 接口用途

获取当前用户可见菜单树。

### 3.5.2 权限要求

需登录。

### 3.5.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `menuId` | bigint | 菜单 ID |
| `menuName` | string | 菜单名称 |
| `path` | string | 路由路径 |
| `component` | string | 前端组件标识 |
| `children` | array | 子菜单 |

## 3.6 `GET /api/v1/auth/me/permissions`

### 3.6.1 接口用途

获取当前用户功能权限和数据权限摘要。

### 3.6.2 权限要求

需登录。

### 3.6.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `permissionCodes` | array | 功能权限码 |
| `dataScopes` | array | 数据范围摘要 |
| `exportPermissions` | array | 导出权限 |

------

# 4 基础数据服务接口明细

## 4.1 `GET /api/v1/master/students`

### 4.1.1 接口用途

分页查询学生列表。

### 4.1.2 权限要求

需登录，且具备学生查询权限和对应数据范围。

### 4.1.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNo` | integer | 是 | 页码 |
| `pageSize` | integer | 是 | 页大小 |
| `keyword` | string | 否 | 学号/姓名关键字 |
| `gradeId` | bigint | 否 | 年级 ID |
| `classId` | bigint | 否 | 班级 ID |
| `studentStatus` | string | 否 | 学籍状态 |

### 4.1.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `studentId` | bigint | 学生 ID |
| `studentNo` | string | 学号 |
| `studentName` | string | 学生姓名 |
| `gender` | string | 性别 |
| `gradeName` | string | 年级名称 |
| `className` | string | 班级名称 |
| `studentStatus` | string | 学籍状态 |

### 4.1.5 备注

1. 数据范围以班级/年级为主；
2. 高频接口，应有缓存和索引支持。

## 4.2 `GET /api/v1/master/students/{id}`

### 4.2.1 接口用途

获取学生详情。

### 4.2.2 权限要求

需登录，且具备查看该学生数据范围的权限。

### 4.2.3 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 学生主键 |

### 4.2.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `studentId` | bigint | 学生 ID |
| `studentNo` | string | 学号 |
| `studentName` | string | 姓名 |
| `gender` | string | 性别 |
| `birthday` | string | 出生日期 |
| `gradeId` | bigint | 年级 ID |
| `classId` | bigint | 班级 ID |
| `guardians` | array | 监护人信息 |

## 4.3 `GET /api/v1/master/teachers`

### 4.3.1 接口用途

分页查询教师列表。

### 4.3.2 权限要求

需登录，且具备教师档案查看权限。

### 4.3.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNo` | integer | 是 | 页码 |
| `pageSize` | integer | 是 | 页大小 |
| `keyword` | string | 否 | 工号/姓名关键字 |
| `orgUnitId` | bigint | 否 | 所属组织单元 |
| `teacherStatus` | string | 否 | 教师状态 |

### 4.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `teacherId` | bigint | 教师 ID |
| `teacherNo` | string | 工号 |
| `teacherName` | string | 教师姓名 |
| `mobile` | string | 手机号，按权限脱敏 |
| `orgUnitName` | string | 组织名称 |
| `teacherStatus` | string | 状态 |

## 4.4 `GET /api/v1/master/classes`

### 4.4.1 接口用途

查询班级列表，供排课、学生管理、收费和审批选择使用。

### 4.4.2 权限要求

需登录。

### 4.4.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `gradeId` | bigint | 否 | 年级 ID |
| `campusId` | bigint | 否 | 校区 ID |
| `status` | string | 否 | 班级状态 |

### 4.4.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `classId` | bigint | 班级 ID |
| `classNo` | string | 班级编码 |
| `className` | string | 班级名称 |
| `gradeName` | string | 年级名称 |
| `headTeacherName` | string | 班主任 |

## 4.5 `GET /api/v1/master/courses`

### 4.5.1 接口用途

查询课程列表。

### 4.5.2 权限要求

需登录。

### 4.5.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `keyword` | string | 否 | 课程编码/名称关键字 |
| `status` | string | 否 | 课程状态 |

### 4.5.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `courseId` | bigint | 课程 ID |
| `courseNo` | string | 课程编码 |
| `courseName` | string | 课程名称 |
| `courseType` | string | 课程类型 |

## 4.6 `POST /api/v1/master/students/import-tasks`

### 4.6.1 接口用途

创建学生导入任务。

### 4.6.2 权限要求

需登录，且具备主数据导入权限。

### 4.6.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `fileId` | bigint | 是 | 上传文件 ID |
| `importMode` | string | 是 | 导入模式 |
| `allowUpdate` | boolean | 否 | 是否允许更新 |

### 4.6.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 导入任务 ID |
| `taskStatus` | string | 任务状态 |

### 4.6.5 幂等要求

同一文件同一导入模式下，应避免重复创建导入任务。

## 4.7 `GET /api/v1/master/dicts/{dictType}`

### 4.7.1 接口用途

查询统一字典项。

### 4.7.2 权限要求

需登录。

### 4.7.3 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `dictType` | string | 是 | 字典类型编码 |

### 4.7.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `itemCode` | string | 字典项编码 |
| `itemName` | string | 字典项名称 |
| `sortNo` | integer | 排序号 |

------

# 5 教务服务接口明细

## 5.1 `GET /api/v1/academic/timetables`

### 5.1.1 接口用途

分页或条件查询课表。

### 5.1.2 权限要求

需登录，且具备教务查看权限。

### 5.1.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `classId` | bigint | 否 | 班级 ID |
| `teacherId` | bigint | 否 | 教师 ID |
| `classroomId` | bigint | 否 | 教室 ID |
| `weekday` | integer | 否 | 星期几 |

### 5.1.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `entryId` | bigint | 课表明细 ID |
| `className` | string | 班级名称 |
| `courseName` | string | 课程名称 |
| `teacherName` | string | 教师名称 |
| `classroomName` | string | 教室名称 |
| `weekday` | integer | 星期几 |
| `periodName` | string | 节次名称 |

## 5.2 `POST /api/v1/academic/timetables/generate`

### 5.2.1 接口用途

创建课表生成任务。

### 5.2.2 权限要求

需登录，且具备排课管理权限。

### 5.2.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `gradeIds` | array | 否 | 目标年级集合 |
| `classIds` | array | 否 | 目标班级集合 |
| `forceRebuild` | boolean | 否 | 是否强制重建 |

### 5.2.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 任务 ID |
| `taskStatus` | string | 状态 |

### 5.2.5 备注

1. 大批量排课必须异步；
2. 若已存在有效课表，应有冲突校验和确认机制。

## 5.3 `GET /api/v1/academic/attendance-records`

### 5.3.1 接口用途

查询考勤记录。

### 5.3.2 权限要求

需登录，且具备考勤查看权限。

### 5.3.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bizDate` | string | 是 | 业务日期 |
| `classId` | bigint | 否 | 班级 ID |
| `studentId` | bigint | 否 | 学生 ID |
| `attendanceStatus` | string | 否 | 出勤状态 |

### 5.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `attendanceId` | bigint | 考勤 ID |
| `studentName` | string | 学生姓名 |
| `className` | string | 班级名称 |
| `courseName` | string | 课程名称 |
| `attendanceStatus` | string | 出勤状态 |

## 5.4 `POST /api/v1/academic/attendance-records`

### 5.4.1 接口用途

提交或更新考勤记录。

### 5.4.2 权限要求

需登录，且具备考勤录入权限。

### 5.4.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `bizDate` | string | 是 | 业务日期 |
| `classId` | bigint | 是 | 班级 ID |
| `courseId` | bigint | 否 | 课程 ID |
| `periodId` | bigint | 否 | 节次 ID |
| `records` | array | 是 | 学生考勤集合 |

`records` 子项建议字段：

1. `studentId`
2. `attendanceStatus`
3. `relatedProcessId`
4. `remark`

### 5.4.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `successCount` | integer | 成功条数 |
| `failCount` | integer | 失败条数 |

## 5.5 `POST /api/v1/academic/grade-tasks`

### 5.5.1 接口用途

创建成绩任务。

### 5.5.2 权限要求

需登录，且具备成绩管理权限。

### 5.5.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `classId` | bigint | 是 | 班级 ID |
| `courseId` | bigint | 是 | 课程 ID |
| `teacherId` | bigint | 否 | 教师 ID |
| `scoreMode` | string | 是 | 评分模式 |

### 5.5.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `gradeTaskId` | bigint | 成绩任务 ID |
| `status` | string | 初始状态 |

## 5.6 `POST /api/v1/academic/grade-records/batch-save`

### 5.6.1 接口用途

批量保存成绩明细。

### 5.6.2 权限要求

需登录，且具备成绩录入权限。

### 5.6.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `gradeTaskId` | bigint | 是 | 成绩任务 ID |
| `records` | array | 是 | 成绩记录集合 |

`records` 子项建议字段：

1. `studentId`
2. `scoreValue`
3. `scoreLevel`
4. `remark`

### 5.6.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `savedCount` | integer | 保存数量 |

## 5.7 `POST /api/v1/academic/grade-tasks/{id}/publish`

### 5.7.1 接口用途

发布成绩。

### 5.7.2 权限要求

需登录，且具备成绩发布权限。

### 5.7.3 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 成绩任务 ID |

### 5.7.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `gradeTaskId` | bigint | 成绩任务 ID |
| `publishStatus` | string | 发布状态 |
| `publishedAt` | string | 发布时间 |

### 5.7.5 触发事件

发布成功后触发 `GradePublished`。

------

# 6 审批服务接口明细

## 6.1 `POST /api/v1/workflow/leave-requests`

### 6.1.1 接口用途

提交请假申请。

### 6.1.2 权限要求

需登录，且具备请假提交流程权限。

### 6.1.3 幂等要求

必须携带 `X-Idempotency-Key`。

### 6.1.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `studentId` | bigint | 是 | 学生 ID |
| `leaveType` | string | 是 | 请假类型 |
| `startDate` | string | 是 | 开始日期 |
| `endDate` | string | 是 | 结束日期 |
| `leaveSection` | string | 否 | 时间段 |
| `reason` | string | 否 | 请假原因 |
| `attachmentIds` | array | 否 | 附件 ID 列表 |

### 6.1.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程实例 ID |
| `processNo` | string | 流程编号 |
| `leaveFormId` | bigint | 请假单 ID |
| `status` | string | 流程状态 |

## 6.2 `POST /api/v1/workflow/leave-requests/{id}/approve`

### 6.2.1 接口用途

请假审批通过。

### 6.2.2 权限要求

需登录，且当前用户必须是该任务办理人。

### 6.2.3 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 请假单或流程主键 |

### 6.2.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | bigint | 是 | 当前任务 ID |
| `comment` | string | 否 | 审批意见 |

### 6.2.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程实例 ID |
| `status` | string | 流程状态 |
| `nextNode` | string | 下一节点，若无则为空 |

### 6.2.6 触发事件

最终通过后触发 `LeaveApproved`。

## 6.3 `POST /api/v1/workflow/leave-requests/{id}/reject`

### 6.3.1 接口用途

请假审批驳回。

### 6.3.2 权限要求

同审批通过接口。

### 6.3.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `taskId` | bigint | 是 | 当前任务 ID |
| `comment` | string | 是 | 驳回原因 |

### 6.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程实例 ID |
| `status` | string | 驳回后状态 |

## 6.4 `POST /api/v1/workflow/schedule-change-requests`

### 6.4.1 接口用途

提交调课申请。

### 6.4.2 权限要求

需登录，且具备调课流程发起权限。

### 6.4.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `classId` | bigint | 是 | 班级 ID |
| `courseId` | bigint | 是 | 课程 ID |
| `teacherId` | bigint | 是 | 教师 ID |
| `originWeekday` | integer | 是 | 原星期 |
| `originPeriodId` | bigint | 是 | 原节次 |
| `targetWeekday` | integer | 是 | 目标星期 |
| `targetPeriodId` | bigint | 是 | 目标节次 |
| `reason` | string | 否 | 调课原因 |

### 6.4.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程实例 ID |
| `scheduleChangeId` | bigint | 调课单 ID |
| `status` | string | 状态 |

## 6.5 `GET /api/v1/workflow/tasks/todo`

### 6.5.1 接口用途

获取当前用户待办列表。

### 6.5.2 权限要求

需登录。

### 6.5.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNo` | integer | 是 | 页码 |
| `pageSize` | integer | 是 | 页大小 |
| `bizType` | string | 否 | 业务类型过滤 |

### 6.5.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 任务 ID |
| `processId` | bigint | 流程 ID |
| `bizType` | string | 业务类型 |
| `title` | string | 待办标题 |
| `createdAt` | string | 创建时间 |
| `dueAt` | string | 截止时间 |

## 6.6 `GET /api/v1/workflow/process-instances/{id}`

### 6.6.1 接口用途

获取流程详情。

### 6.6.2 权限要求

需登录，且具备查看该流程权限。

### 6.6.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程实例 ID |
| `processNo` | string | 流程编号 |
| `bizType` | string | 业务类型 |
| `processStatus` | string | 流程状态 |
| `applicantName` | string | 申请人 |
| `tasks` | array | 节点任务历史 |
| `formData` | object | 表单内容 |

## 6.7 `POST /api/v1/workflow/process-instances/{id}/cancel`

### 6.7.1 接口用途

申请人撤销流程。

### 6.7.2 权限要求

需登录，且必须是流程发起人，并且流程仍可撤销。

### 6.7.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `processId` | bigint | 流程 ID |
| `status` | string | 撤销后状态 |

------

# 7 收费服务接口明细

## 7.1 `POST /api/v1/billing/fee-items`

### 7.1.1 接口用途

创建费用项目。

### 7.1.2 权限要求

需登录，且具备费用项目维护权限。

### 7.1.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `feeItemName` | string | 是 | 项目名称 |
| `feeCategory` | string | 是 | 费用类别 |
| `billingMode` | string | 是 | 计费模式 |
| `amount` | number | 否 | 标准金额 |
| `termId` | bigint | 否 | 适用学期 |
| `status` | string | 是 | 状态 |

### 7.1.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `feeItemId` | bigint | 费用项目 ID |
| `feeItemNo` | string | 费用项目编码 |

## 7.2 `POST /api/v1/billing/bills/generate`

### 7.2.1 接口用途

创建账单生成任务。

### 7.2.2 权限要求

需登录，且具备账单生成权限。

### 7.2.3 幂等要求

必须携带 `X-Idempotency-Key`。

### 7.2.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `termId` | bigint | 是 | 学期 ID |
| `feeItemId` | bigint | 是 | 费用项目 ID |
| `classIds` | array | 否 | 班级集合 |
| `studentIds` | array | 否 | 学生集合 |

### 7.2.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 任务 ID |
| `taskStatus` | string | 任务状态 |

### 7.2.6 触发事件

账单生成成功后触发 `BillGenerated`。

## 7.3 `GET /api/v1/billing/bills`

### 7.3.1 接口用途

分页查询账单列表。

### 7.3.2 权限要求

需登录，且具备账单查看权限和财务数据范围。

### 7.3.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `pageNo` | integer | 是 | 页码 |
| `pageSize` | integer | 是 | 页大小 |
| `studentId` | bigint | 否 | 学生 ID |
| `classId` | bigint | 否 | 班级 ID |
| `billStatus` | string | 否 | 账单状态 |
| `feeItemId` | bigint | 否 | 费用项目 ID |

### 7.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `billId` | bigint | 账单 ID |
| `billNo` | string | 账单号 |
| `studentName` | string | 学生姓名 |
| `feeItemName` | string | 费用项目 |
| `receivableAmount` | number | 应收金额 |
| `receivedAmount` | number | 已收金额 |
| `billStatus` | string | 状态 |

## 7.4 `GET /api/v1/billing/bills/{id}`

### 7.4.1 接口用途

获取账单详情。

### 7.4.2 权限要求

需登录。

### 7.4.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `billId` | bigint | 账单 ID |
| `billNo` | string | 账单号 |
| `studentInfo` | object | 学生信息 |
| `feeItemInfo` | object | 费用项信息 |
| `billDetails` | array | 账单明细 |
| `receipts` | array | 收款记录 |
| `refunds` | array | 退费记录 |

## 7.5 `POST /api/v1/billing/receipts`

### 7.5.1 接口用途

创建收款记录。

### 7.5.2 权限要求

需登录，且具备收款登记权限。

### 7.5.3 幂等要求

如为回调转人工登记统一入口，必须传幂等键。

### 7.5.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `billId` | bigint | 是 | 账单 ID |
| `receiptAmount` | number | 是 | 收款金额 |
| `paymentChannel` | string | 是 | 支付渠道 |
| `paymentTime` | string | 否 | 支付时间 |
| `sourceType` | string | 是 | 来源类型 |

### 7.5.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `receiptId` | bigint | 收款 ID |
| `receiptNo` | string | 收款单号 |
| `billStatus` | string | 更新后的账单状态 |

### 7.5.6 触发事件

收款成功后触发 `PaymentCompleted`。

## 7.6 `POST /api/v1/billing/refunds/{id}/confirm`

### 7.6.1 接口用途

确认退费。

### 7.6.2 权限要求

需登录，且具备退费处理权限。

### 7.6.3 路径参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 退费单 ID |

### 7.6.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `refundAmount` | number | 是 | 退费金额 |
| `comment` | string | 否 | 说明 |

### 7.6.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `refundId` | bigint | 退费单 ID |
| `refundStatus` | string | 退费状态 |

## 7.7 `POST /api/v1/billing/payment/callback`

### 7.7.1 接口用途

接收第三方支付回调。

### 7.7.2 权限要求

公共接口，但必须验证签名与来源。

### 7.7.3 请求参数

按第三方协议，但内部至少需映射：

1. `tradeNo`
2. `outTradeNo`
3. `amount`
4. `tradeStatus`
5. `sign`
6. `callbackTime`

### 7.7.4 响应字段

按第三方协议返回成功或失败确认。

### 7.7.5 特殊规则

1. 必须幂等；
2. 必须保存原始回调报文；
3. 重复回调不得重复记账。

------

# 8 消息通知服务接口明细

## 8.1 `POST /api/v1/notify/messages/send`

### 8.1.1 接口用途

创建消息发送任务。

### 8.1.2 权限要求

内部服务调用为主，也可由后台管理端调用测试接口；必须鉴权。

### 8.1.3 幂等要求

必须支持幂等键。

### 8.1.4 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bizType` | string | 是 | 业务类型 |
| `bizId` | bigint | 是 | 业务主键 |
| `templateCode` | string | 是 | 模板编码 |
| `channelType` | string | 是 | 渠道类型 |
| `recipientIds` | array | 是 | 接收人 ID 列表 |
| `templateParams` | object | 否 | 模板参数 |
| `idempotencyKey` | string | 否 | 幂等键 |

### 8.1.5 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 任务 ID |
| `taskNo` | string | 任务编号 |
| `taskStatus` | string | 任务状态 |

## 8.2 `GET /api/v1/notify/messages/{taskId}`

### 8.2.1 接口用途

查询消息任务详情。

### 8.2.2 权限要求

需登录，且具备消息查看权限。

### 8.2.3 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `taskId` | bigint | 任务 ID |
| `taskNo` | string | 任务编号 |
| `bizType` | string | 业务类型 |
| `channelType` | string | 渠道类型 |
| `taskStatus` | string | 任务状态 |
| `successCount` | integer | 成功数 |
| `failCount` | integer | 失败数 |

## 8.3 `GET /api/v1/notify/messages/{taskId}/recipients`

### 8.3.1 接口用途

查询消息任务接收人明细。

### 8.3.2 权限要求

需登录，且具备消息详情权限。

### 8.3.3 请求参数

分页参数通用。

### 8.3.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `recipientId` | bigint | 接收人 ID |
| `recipientName` | string | 接收人名称 |
| `sendStatus` | string | 发送状态 |
| `failReason` | string | 失败原因 |

## 8.4 `POST /api/v1/notify/templates`

### 8.4.1 接口用途

创建消息模板。

### 8.4.2 权限要求

需登录，且具备模板维护权限。

### 8.4.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `templateCode` | string | 是 | 模板编码 |
| `templateName` | string | 是 | 模板名称 |
| `bizType` | string | 是 | 业务类型 |
| `channelType` | string | 是 | 渠道类型 |
| `titleTemplate` | string | 否 | 标题模板 |
| `contentTemplate` | string | 是 | 内容模板 |
| `status` | string | 是 | 模板状态 |

### 8.4.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `templateId` | bigint | 模板 ID |
| `templateCode` | string | 模板编码 |

## 8.5 `GET /api/v1/notify/templates`

### 8.5.1 接口用途

查询模板列表。

### 8.5.2 权限要求

需登录。

### 8.5.3 请求参数

| 参数名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `bizType` | string | 否 | 业务类型 |
| `channelType` | string | 否 | 渠道类型 |
| `status` | string | 否 | 模板状态 |

### 8.5.4 响应字段

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `templateId` | bigint | 模板 ID |
| `templateCode` | string | 模板编码 |
| `templateName` | string | 模板名称 |
| `bizType` | string | 业务类型 |
| `channelType` | string | 渠道类型 |
| `status` | string | 状态 |

------

# 9 关键接口与事件映射

| 触发动作 | 触发接口 | 后续事件 |
| --- | --- | --- |
| 请假审批通过 | `POST /api/v1/workflow/leave-requests/{id}/approve` | `LeaveApproved` |
| 调课审批通过 | 调课审批通过接口 | `ScheduleAdjusted` |
| 账单生成完成 | `POST /api/v1/billing/bills/generate` 后台任务完成 | `BillGenerated` |
| 收款成功 | `POST /api/v1/billing/receipts` / 支付回调 | `PaymentCompleted` |
| 成绩发布 | `POST /api/v1/academic/grade-tasks/{id}/publish` | `GradePublished` |
| 主数据变更 | 主数据新增/修改接口 | `MasterDataChanged` |

------

# 10 联调与测试建议

## 10.1 联调优先级

一期建议按以下顺序联调：

1. 登录和权限链路；
2. 主数据查询；
3. 课表与考勤；
4. 请假流程与考勤联动；
5. 账单与收款；
6. 成绩发布；
7. 消息通知；
8. 外部回调。

## 10.2 测试重点

1. 权限不足场景；
2. 数据范围不足场景；
3. 状态流转非法场景；
4. 幂等重复提交场景；
5. 第三方回调重复推送场景；
6. 消息发送失败重试场景。

------

# 11 结论

本文档把学校 ERP 一期最核心的服务接口从“规范级”进一步落实到了“清单级、联调级、任务分配级”的粒度。后续如果要继续扩展到真正的开发交付版本，可以在此基础上继续补充：

1. 每个接口的完整 OpenAPI schema；
2. 成功/失败/异常示例报文；
3. 对应错误码矩阵；
4. 自动化测试用例编号；
5. 前端页面与接口映射关系。

在现阶段，这份接口清单已经可以作为开发排期、接口评审和联调准备的直接输入材料。

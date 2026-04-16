# 学校 ERP 系统

## 详细表结构设计

**项目名称：** 学校 ERP 系统一期建设项目  
**文档名称：** 详细表结构设计  
**版本号：** V1.0  
**编写日期：** 2026-04-16  
**文档状态：** 设计版  

------

# 1 引言

## 1.1 编写目的

本文档在《数据库总体设计说明书》的基础上，进一步细化学校 ERP 一期项目的核心数据表结构设计，明确各领域核心表的字段组成、主键策略、业务编码规则、索引建议、唯一约束、状态字段、快照字段、表间关系以及设计注意事项，为后续数据库 DDL 编写、后端实体建模、接口开发、数据迁移和测试造数提供直接依据。

## 1.2 适用范围

本文档重点覆盖以下领域的核心表：

1. 认证权限域；
2. 基础数据域；
3. 教务域；
4. 审批域；
5. 收费域；
6. 消息通知域。

说明如下：

1. 本文档优先展开核心主表和高频交易表；
2. 纯关系表、纯日志表、纯字典表会给出设计要点，但不逐张展开到同等细度；
3. 所列字段为建议结构，可在不破坏总体边界和命名规范的前提下微调；
4. 最终落库时应结合 PostgreSQL 版本、框架特性和性能测试结果做细化。

## 1.3 关联文档

1. 《系统总体架构设计说明书》
2. 《微服务拆分与服务边界清单》
3. 《数据库总体设计说明书》
4. 《接口设计规范说明书》

------

# 2 设计基线

## 2.1 命名规范

### 2.1.1 表命名规范

1. 表名统一使用小写加下划线；
2. 采用“域前缀 + 业务语义”的命名方式；
3. 主数据域采用 `md_` 前缀；
4. 教务域采用 `ac_` 前缀；
5. 审批域采用 `wf_` 前缀；
6. 收费域采用 `bl_` 前缀；
7. 消息域采用 `nt_` 前缀；
8. 认证权限域采用 `sys_` 前缀。

示例：

1. `auth.sys_user`
2. `master.md_student`
3. `academic.ac_attendance_record`
4. `workflow.wf_process_instance`
5. `billing.bl_bill`
6. `notify.nt_message_task`

### 2.1.2 字段命名规范

1. 字段名统一使用小写加下划线；
2. 主键字段统一使用 `id`；
3. 外键字段统一使用 `{实体}_id`；
4. 业务编码统一使用 `{实体}_no`；
5. 状态字段统一使用 `{业务}_status` 或 `status`；
6. 时间字段统一使用 `_at`、`_date`、`_time` 后缀；
7. 快照字段统一使用 `_snapshot` 后缀。

## 2.2 通用字段基线

除少量纯关系表或流水表外，核心业务表建议统一包含以下字段：

| 字段名 | 类型建议 | 说明 |
| --- | --- | --- |
| `id` | bigint | 技术主键 |
| `created_by` | bigint | 创建人 ID |
| `created_at` | timestamp | 创建时间 |
| `updated_by` | bigint | 更新人 ID |
| `updated_at` | timestamp | 更新时间 |
| `version_no` | integer | 乐观锁版本号 |
| `is_deleted` | boolean | 逻辑删除标记 |
| `remark` | varchar(500) | 备注 |

## 2.3 状态字段设计基线

1. 状态字段必须表达单一语义；
2. 状态值建议使用字符串枚举或整型字典，但接口层应统一输出枚举语义；
3. 高风险状态流转应配套状态变更日志；
4. 不允许多个布尔字段共同表示一个业务状态。

## 2.4 关系与快照基线

1. 交易表引用主数据时优先使用 ID；
2. 涉及历史追溯的名称信息需要冗余快照；
3. 快照字段只用于展示和审计，不参与主数据主版本维护；
4. 跨域关系不使用数据库层级联更新。

------

# 3 认证权限域表设计

## 3.1 `auth.sys_user`

### 3.1.1 表用途

系统用户主表，用于维护可登录系统的账号基础信息。

### 3.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 用户主键 |
| `user_no` | varchar(32) | 是 | 用户编码 |
| `username` | varchar(64) | 是 | 登录账号 |
| `password_hash` | varchar(255) | 是 | 密码密文 |
| `display_name` | varchar(64) | 是 | 显示名称 |
| `user_type` | varchar(32) | 是 | 用户类型，如 `ADMIN`、`TEACHER` |
| `mobile` | varchar(32) | 否 | 手机号 |
| `email` | varchar(128) | 否 | 邮箱 |
| `bind_teacher_id` | bigint | 否 | 绑定教师 ID |
| `bind_student_id` | bigint | 否 | 绑定学生 ID，家长/学生端预留 |
| `org_unit_id` | bigint | 否 | 所属组织单元 ID |
| `status` | varchar(32) | 是 | 状态，如 `ENABLED`、`DISABLED`、`LOCKED` |
| `last_login_at` | timestamp | 否 | 最近登录时间 |
| `password_changed_at` | timestamp | 否 | 最近改密时间 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |
| `remark` | varchar(500) | 否 | 备注 |

### 3.1.3 索引与约束建议

1. `pk_sys_user(id)`
2. `uk_sys_user_user_no(user_no)`
3. `uk_sys_user_username(username)`
4. `idx_sys_user_bind_teacher(bind_teacher_id)`
5. `idx_sys_user_org_status(org_unit_id, status)`

### 3.1.4 设计说明

1. `username` 必须唯一；
2. `password_hash` 只存密文，不存明文；
3. 教师账号与教师主档案通过 `bind_teacher_id` 关联；
4. 不建议在用户表中冗余保存教师/学生完整主档案。

## 3.2 `auth.sys_role`

### 3.2.1 表用途

角色主表，用于定义系统内角色集合。

### 3.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 角色主键 |
| `role_code` | varchar(64) | 是 | 角色编码 |
| `role_name` | varchar(64) | 是 | 角色名称 |
| `role_type` | varchar(32) | 是 | 角色类型 |
| `status` | varchar(32) | 是 | 启停状态 |
| `sort_no` | integer | 否 | 排序号 |
| `description` | varchar(500) | 否 | 角色说明 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 3.2.3 索引与约束建议

1. `pk_sys_role(id)`
2. `uk_sys_role_code(role_code)`
3. `uk_sys_role_name(role_name)`
4. `idx_sys_role_status(status)`

### 3.2.4 设计说明

1. 角色编码必须稳定，用于权限配置和数据初始化；
2. 角色名称可展示但不建议作为程序判断依据；
3. 高风险角色的变更必须配套审计日志。

## 3.3 `auth.sys_login_session`

### 3.3.1 表用途

登录会话表，用于保存当前登录态和会话控制信息。

### 3.3.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 会话主键 |
| `session_no` | varchar(64) | 是 | 会话编号 |
| `user_id` | bigint | 是 | 用户 ID |
| `access_token_hash` | varchar(255) | 是 | 访问令牌摘要 |
| `refresh_token_hash` | varchar(255) | 否 | 刷新令牌摘要 |
| `client_type` | varchar(32) | 是 | 客户端类型 |
| `device_info` | varchar(255) | 否 | 设备信息 |
| `ip_address` | varchar(64) | 否 | 登录 IP |
| `login_at` | timestamp | 是 | 登录时间 |
| `expired_at` | timestamp | 是 | 过期时间 |
| `logout_at` | timestamp | 否 | 登出时间 |
| `session_status` | varchar(32) | 是 | 会话状态 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

### 3.3.3 索引与约束建议

1. `pk_sys_login_session(id)`
2. `uk_sys_login_session_no(session_no)`
3. `idx_sys_login_session_user(user_id, session_status)`
4. `idx_sys_login_session_expired(expired_at)`

### 3.3.4 设计说明

1. 令牌建议只保存摘要，不保存明文；
2. 会话状态可取 `ACTIVE`、`LOGGED_OUT`、`EXPIRED`、`KICKED_OUT`；
3. 可与 Redis 中的会话缓存配合使用。

------

# 4 基础数据域表设计

## 4.1 `master.md_class`

### 4.1.1 表用途

班级主档案表。

### 4.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 班级主键 |
| `class_no` | varchar(32) | 是 | 班级编码 |
| `class_name` | varchar(64) | 是 | 班级名称 |
| `school_id` | bigint | 是 | 学校 ID |
| `campus_id` | bigint | 是 | 校区 ID |
| `grade_id` | bigint | 是 | 年级 ID |
| `org_unit_id` | bigint | 否 | 组织单元 ID |
| `head_teacher_id` | bigint | 否 | 班主任教师 ID |
| `entry_year` | integer | 否 | 入学年份 |
| `student_capacity` | integer | 否 | 班级容量 |
| `status` | varchar(32) | 是 | 班级状态 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |
| `remark` | varchar(500) | 否 | 备注 |

### 4.1.3 索引与约束建议

1. `pk_md_class(id)`
2. `uk_md_class_no(class_no)`
3. `uk_md_class_grade_name(grade_id, class_name)`
4. `idx_md_class_campus_status(campus_id, status)`
5. `idx_md_class_head_teacher(head_teacher_id)`

### 4.1.4 设计说明

1. 同一年级下班级名称建议唯一；
2. `head_teacher_id` 引用教师主档案；
3. 班级停用不代表历史业务数据失效。

## 4.2 `master.md_teacher`

### 4.2.1 表用途

教师主档案表。

### 4.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 教师主键 |
| `teacher_no` | varchar(32) | 是 | 工号 |
| `teacher_name` | varchar(64) | 是 | 教师姓名 |
| `gender` | varchar(16) | 否 | 性别 |
| `mobile` | varchar(32) | 否 | 手机号 |
| `email` | varchar(128) | 否 | 邮箱 |
| `id_card_no` | varchar(64) | 否 | 身份证号，可加密 |
| `school_id` | bigint | 是 | 学校 ID |
| `campus_id` | bigint | 否 | 校区 ID |
| `org_unit_id` | bigint | 否 | 组织单元 ID |
| `hire_date` | date | 否 | 入职日期 |
| `teacher_status` | varchar(32) | 是 | 教师状态 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |
| `remark` | varchar(500) | 否 | 备注 |

### 4.2.3 索引与约束建议

1. `pk_md_teacher(id)`
2. `uk_md_teacher_no(teacher_no)`
3. `idx_md_teacher_org_status(org_unit_id, teacher_status)`
4. `idx_md_teacher_mobile(mobile)`

### 4.2.4 设计说明

1. 工号必须唯一；
2. 身份证号等敏感字段建议加密存储；
3. 教师停用后，其历史课表、审批、成绩记录仍需保留。

## 4.3 `master.md_student`

### 4.3.1 表用途

学生主档案表。

### 4.3.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 学生主键 |
| `student_no` | varchar(32) | 是 | 学号 |
| `student_name` | varchar(64) | 是 | 学生姓名 |
| `gender` | varchar(16) | 否 | 性别 |
| `birthday` | date | 否 | 出生日期 |
| `id_card_no` | varchar(64) | 否 | 身份证号，可加密 |
| `school_id` | bigint | 是 | 学校 ID |
| `campus_id` | bigint | 否 | 校区 ID |
| `grade_id` | bigint | 是 | 年级 ID |
| `class_id` | bigint | 是 | 班级 ID |
| `admission_date` | date | 否 | 入学日期 |
| `student_status` | varchar(32) | 是 | 学籍状态 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |
| `remark` | varchar(500) | 否 | 备注 |

### 4.3.3 索引与约束建议

1. `pk_md_student(id)`
2. `uk_md_student_no(student_no)`
3. `idx_md_student_class_status(class_id, student_status)`
4. `idx_md_student_grade(grade_id)`
5. `idx_md_student_name(student_name)`

### 4.3.4 设计说明

1. 学号必须唯一；
2. 学生调班时更新 `class_id`，必要时应配套变更日志；
3. 高风险字段如身份证号建议脱敏展示。

## 4.4 `master.md_course`

### 4.4.1 表用途

课程主档案表。

### 4.4.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 课程主键 |
| `course_no` | varchar(32) | 是 | 课程编码 |
| `course_name` | varchar(64) | 是 | 课程名称 |
| `course_type` | varchar(32) | 否 | 课程类型 |
| `credit` | numeric(6,2) | 否 | 学分，预留 |
| `school_id` | bigint | 是 | 学校 ID |
| `status` | varchar(32) | 是 | 课程状态 |
| `sort_no` | integer | 否 | 排序号 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 4.4.3 索引与约束建议

1. `pk_md_course(id)`
2. `uk_md_course_no(course_no)`
3. `uk_md_course_name_school(school_id, course_name)`
4. `idx_md_course_status(status)`

### 4.4.4 设计说明

1. 课程编码建议稳定，用于排课和成绩管理；
2. 同校课程名称原则上唯一；
3. 课程停用后不应影响历史课表和成绩记录展示。

## 4.5 `master.md_term`

### 4.5.1 表用途

学期主档案表。

### 4.5.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 学期主键 |
| `term_no` | varchar(32) | 是 | 学期编码 |
| `term_name` | varchar(64) | 是 | 学期名称 |
| `school_year` | varchar(32) | 是 | 学年 |
| `school_id` | bigint | 是 | 学校 ID |
| `start_date` | date | 是 | 开始日期 |
| `end_date` | date | 是 | 结束日期 |
| `term_status` | varchar(32) | 是 | 学期状态 |
| `is_current` | boolean | 是 | 是否当前学期 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 4.5.3 索引与约束建议

1. `pk_md_term(id)`
2. `uk_md_term_no(term_no)`
3. `uk_md_term_school_name(school_id, term_name)`
4. `idx_md_term_current(school_id, is_current)`

### 4.5.4 设计说明

1. 同一学校只能有一个当前学期；
2. 学期日期区间不应重叠；
3. 学期是课表、成绩、收费规则的重要维度。

------

# 5 教务域表设计

## 5.1 `academic.ac_timetable_entry`

### 5.1.1 表用途

课表明细表，记录某学期具体班级、课程、教师、教室、星期和节次的安排。

### 5.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 课表明细主键 |
| `timetable_id` | bigint | 是 | 课表主表 ID |
| `term_id` | bigint | 是 | 学期 ID |
| `grade_id` | bigint | 否 | 年级 ID |
| `class_id` | bigint | 是 | 班级 ID |
| `course_id` | bigint | 是 | 课程 ID |
| `teacher_id` | bigint | 是 | 教师 ID |
| `classroom_id` | bigint | 否 | 教室 ID |
| `weekday` | integer | 是 | 星期几，1-7 |
| `period_id` | bigint | 是 | 节次 ID |
| `start_time` | time | 否 | 上课开始时间 |
| `end_time` | time | 否 | 上课结束时间 |
| `entry_status` | varchar(32) | 是 | 课表状态 |
| `source_type` | varchar(32) | 否 | 来源类型，如自动排课/手工调整 |
| `course_name_snapshot` | varchar(64) | 否 | 课程名称快照 |
| `teacher_name_snapshot` | varchar(64) | 否 | 教师名称快照 |
| `class_name_snapshot` | varchar(64) | 否 | 班级名称快照 |
| `classroom_name_snapshot` | varchar(64) | 否 | 教室名称快照 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 5.1.3 索引与约束建议

1. `pk_ac_timetable_entry(id)`
2. `idx_ac_timetable_entry_class(term_id, class_id, weekday, period_id)`
3. `idx_ac_timetable_entry_teacher(term_id, teacher_id, weekday, period_id)`
4. `idx_ac_timetable_entry_classroom(term_id, classroom_id, weekday, period_id)`
5. `uk_ac_timetable_entry_unique_slot(term_id, class_id, weekday, period_id, is_deleted)`

### 5.1.4 设计说明

1. 同一学期同一班级同一星期同一节次原则上只能有一条有效记录；
2. 教师和教室的冲突校验既可通过唯一约束也可通过业务校验保障；
3. `source_type` 用于区分自动排课、人工调课、审批驱动调课等来源。

## 5.2 `academic.ac_attendance_record`

### 5.2.1 表用途

学生考勤记录表。

### 5.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 考勤记录主键 |
| `attendance_no` | varchar(32) | 是 | 考勤记录编号 |
| `term_id` | bigint | 是 | 学期 ID |
| `biz_date` | date | 是 | 业务日期 |
| `class_id` | bigint | 是 | 班级 ID |
| `student_id` | bigint | 是 | 学生 ID |
| `course_id` | bigint | 否 | 课程 ID |
| `teacher_id` | bigint | 否 | 任课教师 ID |
| `period_id` | bigint | 否 | 节次 ID |
| `attendance_status` | varchar(32) | 是 | 出勤状态 |
| `leave_source` | varchar(32) | 否 | 请假来源 |
| `related_process_id` | bigint | 否 | 关联审批流程 ID |
| `student_name_snapshot` | varchar(64) | 否 | 学生姓名快照 |
| `class_name_snapshot` | varchar(64) | 否 | 班级名称快照 |
| `recorded_by` | bigint | 否 | 登记人 |
| `recorded_at` | timestamp | 否 | 登记时间 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 5.2.3 索引与约束建议

1. `pk_ac_attendance_record(id)`
2. `uk_ac_attendance_no(attendance_no)`
3. `idx_ac_attendance_student_date(student_id, biz_date)`
4. `idx_ac_attendance_class_date(class_id, biz_date)`
5. `idx_ac_attendance_process(related_process_id)`

### 5.2.4 设计说明

1. 请假审批通过后可通过 `related_process_id` 追溯来源；
2. `attendance_status` 可取 `NORMAL`、`LATE`、`ABSENT`、`LEAVE`；
3. 日统计报表建议读取汇总表，不直接高频扫描明细表。

## 5.3 `academic.ac_grade_record`

### 5.3.1 表用途

学生成绩记录表。

### 5.3.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 成绩记录主键 |
| `grade_task_id` | bigint | 是 | 成绩任务 ID |
| `term_id` | bigint | 是 | 学期 ID |
| `class_id` | bigint | 是 | 班级 ID |
| `student_id` | bigint | 是 | 学生 ID |
| `course_id` | bigint | 是 | 课程 ID |
| `teacher_id` | bigint | 否 | 教师 ID |
| `score_value` | numeric(6,2) | 否 | 分数值 |
| `score_level` | varchar(32) | 否 | 等级制结果 |
| `score_status` | varchar(32) | 是 | 状态，如草稿/已审核/已发布 |
| `student_name_snapshot` | varchar(64) | 否 | 学生姓名快照 |
| `course_name_snapshot` | varchar(64) | 否 | 课程名称快照 |
| `term_name_snapshot` | varchar(64) | 否 | 学期名称快照 |
| `entered_by` | bigint | 否 | 录入人 |
| `entered_at` | timestamp | 否 | 录入时间 |
| `audited_by` | bigint | 否 | 审核人 |
| `audited_at` | timestamp | 否 | 审核时间 |
| `published_at` | timestamp | 否 | 发布时间 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 5.3.3 索引与约束建议

1. `pk_ac_grade_record(id)`
2. `uk_ac_grade_unique(grade_task_id, student_id, course_id)`
3. `idx_ac_grade_class_course(term_id, class_id, course_id)`
4. `idx_ac_grade_student(student_id, term_id)`
5. `idx_ac_grade_status(score_status)`

### 5.3.4 设计说明

1. 同一成绩任务下，单学生单课程只能有一条有效成绩；
2. 分数值与等级值可以同时保留，以兼容不同评分方式；
3. 成绩发布后应保留发布时间并同步消息事件。

------

# 6 审批域表设计

## 6.1 `workflow.wf_process_instance`

### 6.1.1 表用途

流程实例主表，用于维护每一条审批流程的总体状态。

### 6.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 流程实例主键 |
| `process_no` | varchar(32) | 是 | 流程编号 |
| `template_id` | bigint | 是 | 流程模板 ID |
| `biz_type` | varchar(32) | 是 | 业务类型，如 `LEAVE`、`SCHEDULE_CHANGE` |
| `biz_id` | bigint | 否 | 关联业务表单 ID |
| `applicant_id` | bigint | 是 | 申请人 ID |
| `applicant_name_snapshot` | varchar(64) | 否 | 申请人姓名快照 |
| `org_unit_id` | bigint | 否 | 组织单元 ID |
| `current_node_code` | varchar(64) | 否 | 当前节点编码 |
| `process_status` | varchar(32) | 是 | 流程状态 |
| `submitted_at` | timestamp | 否 | 提交时间 |
| `finished_at` | timestamp | 否 | 结束时间 |
| `related_service` | varchar(32) | 否 | 下游关联服务 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 6.1.3 索引与约束建议

1. `pk_wf_process_instance(id)`
2. `uk_wf_process_no(process_no)`
3. `idx_wf_process_applicant(applicant_id, process_status)`
4. `idx_wf_process_biz(biz_type, biz_id)`
5. `idx_wf_process_status(process_status, submitted_at)`

### 6.1.4 设计说明

1. 流程实例是审批域的主枢纽表；
2. `biz_type + biz_id` 用于定位具体业务表单；
3. 流程是否通过由 `process_status` 表达，不能由任务表间接推导。

## 6.2 `workflow.wf_process_task`

### 6.2.1 表用途

流程任务表，用于维护待办、已办、转办等任务状态。

### 6.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 任务主键 |
| `task_no` | varchar(32) | 是 | 任务编号 |
| `process_instance_id` | bigint | 是 | 流程实例 ID |
| `node_code` | varchar(64) | 是 | 节点编码 |
| `node_name` | varchar(64) | 是 | 节点名称 |
| `assignee_id` | bigint | 是 | 办理人 ID |
| `assignee_name_snapshot` | varchar(64) | 否 | 办理人姓名快照 |
| `task_status` | varchar(32) | 是 | 任务状态 |
| `task_result` | varchar(32) | 否 | 任务结果 |
| `received_at` | timestamp | 否 | 接收时间 |
| `handled_at` | timestamp | 否 | 处理时间 |
| `due_at` | timestamp | 否 | 截止时间 |
| `action_comment` | varchar(500) | 否 | 处理意见 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

### 6.2.3 索引与约束建议

1. `pk_wf_process_task(id)`
2. `uk_wf_task_no(task_no)`
3. `idx_wf_task_assignee(assignee_id, task_status, created_at)`
4. `idx_wf_task_process(process_instance_id)`
5. `idx_wf_task_due(due_at, task_status)`

### 6.2.4 设计说明

1. 待办列表高频读取，应优先优化 `assignee_id + task_status` 索引；
2. 任务状态可取 `TODO`、`DONE`、`REJECTED`、`CANCELLED`；
3. 审批意见建议保留在任务表和动作日志表中双重留痕。

## 6.3 `workflow.wf_leave_form`

### 6.3.1 表用途

请假业务表单。

### 6.3.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 请假单主键 |
| `leave_no` | varchar(32) | 是 | 请假单号 |
| `process_instance_id` | bigint | 是 | 流程实例 ID |
| `student_id` | bigint | 是 | 学生 ID |
| `student_name_snapshot` | varchar(64) | 否 | 学生姓名快照 |
| `class_id` | bigint | 是 | 班级 ID |
| `class_name_snapshot` | varchar(64) | 否 | 班级名称快照 |
| `leave_type` | varchar(32) | 是 | 请假类型 |
| `start_date` | date | 是 | 开始日期 |
| `end_date` | date | 是 | 结束日期 |
| `leave_section` | varchar(32) | 否 | 时间段，如上午/下午/全天 |
| `reason` | varchar(500) | 否 | 请假原因 |
| `leave_status` | varchar(32) | 是 | 请假状态 |
| `attachment_count` | integer | 否 | 附件数量 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 6.3.3 索引与约束建议

1. `pk_wf_leave_form(id)`
2. `uk_wf_leave_no(leave_no)`
3. `uk_wf_leave_process(process_instance_id)`
4. `idx_wf_leave_student(student_id, start_date, end_date)`
5. `idx_wf_leave_class(class_id, leave_status)`

### 6.3.4 设计说明

1. 请假单与流程实例一对一；
2. 审批通过后由审批服务发布 `LeaveApproved` 事件；
3. 请假单只表达流程业务，不直接修改考勤结果。

------

# 7 收费域表设计

## 7.1 `billing.bl_fee_item`

### 7.1.1 表用途

费用项目主表。

### 7.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 费用项目主键 |
| `fee_item_no` | varchar(32) | 是 | 费用项目编码 |
| `fee_item_name` | varchar(64) | 是 | 费用项目名称 |
| `fee_category` | varchar(32) | 是 | 费用类别 |
| `billing_mode` | varchar(32) | 是 | 计费模式 |
| `amount` | numeric(18,2) | 否 | 标准金额 |
| `currency_code` | varchar(16) | 是 | 币种 |
| `school_id` | bigint | 是 | 学校 ID |
| `term_id` | bigint | 否 | 适用学期 ID |
| `status` | varchar(32) | 是 | 项目状态 |
| `sort_no` | integer | 否 | 排序号 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 7.1.3 索引与约束建议

1. `pk_bl_fee_item(id)`
2. `uk_bl_fee_item_no(fee_item_no)`
3. `uk_bl_fee_item_name_school(school_id, fee_item_name)`
4. `idx_bl_fee_item_status(status)`

### 7.1.4 设计说明

1. 费用项目用于驱动账单生成；
2. 标准金额允许为空，以兼容规则化计费；
3. 停用费用项目不应影响历史账单。

## 7.2 `billing.bl_bill`

### 7.2.1 表用途

账单主表。

### 7.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 账单主键 |
| `bill_no` | varchar(32) | 是 | 账单号 |
| `student_id` | bigint | 是 | 学生 ID |
| `student_name_snapshot` | varchar(64) | 否 | 学生姓名快照 |
| `class_id` | bigint | 否 | 班级 ID |
| `class_name_snapshot` | varchar(64) | 否 | 班级名称快照 |
| `term_id` | bigint | 否 | 学期 ID |
| `fee_item_id` | bigint | 是 | 费用项目 ID |
| `fee_item_name_snapshot` | varchar(64) | 否 | 费用项目名称快照 |
| `receivable_amount` | numeric(18,2) | 是 | 应收金额 |
| `discount_amount` | numeric(18,2) | 否 | 优惠金额 |
| `received_amount` | numeric(18,2) | 否 | 已收金额 |
| `refund_amount` | numeric(18,2) | 否 | 已退金额 |
| `bill_status` | varchar(32) | 是 | 账单状态 |
| `due_date` | date | 否 | 截止日期 |
| `generated_at` | timestamp | 否 | 生成时间 |
| `closed_at` | timestamp | 否 | 关闭时间 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 7.2.3 索引与约束建议

1. `pk_bl_bill(id)`
2. `uk_bl_bill_no(bill_no)`
3. `idx_bl_bill_student(student_id, bill_status, due_date)`
4. `idx_bl_bill_term_fee(term_id, fee_item_id)`
5. `idx_bl_bill_status_generated(bill_status, generated_at)`

### 7.2.4 设计说明

1. 账单金额字段必须支持后续对账；
2. 账单状态可取 `PENDING`、`PARTIAL_PAID`、`PAID`、`CLOSED`；
3. 历史快照字段用于保障主数据变更后账单展示仍稳定。

## 7.3 `billing.bl_receipt`

### 7.3.1 表用途

收款记录表。

### 7.3.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 收款记录主键 |
| `receipt_no` | varchar(32) | 是 | 收款单号 |
| `bill_id` | bigint | 是 | 账单 ID |
| `payment_txn_id` | bigint | 否 | 支付流水 ID |
| `receipt_amount` | numeric(18,2) | 是 | 收款金额 |
| `payment_channel` | varchar(32) | 是 | 支付渠道 |
| `payment_time` | timestamp | 否 | 支付时间 |
| `receipt_status` | varchar(32) | 是 | 收款状态 |
| `operator_id` | bigint | 否 | 经办人 |
| `source_type` | varchar(32) | 否 | 来源类型，如人工/回调 |
| `idempotency_key` | varchar(128) | 否 | 幂等键 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 7.3.3 索引与约束建议

1. `pk_bl_receipt(id)`
2. `uk_bl_receipt_no(receipt_no)`
3. `uk_bl_receipt_idempotent(idempotency_key)`
4. `idx_bl_receipt_bill(bill_id, receipt_status)`
5. `idx_bl_receipt_payment_time(payment_time)`

### 7.3.4 设计说明

1. 支付回调落账必须走幂等控制；
2. 收款记录与账单是一对多关系；
3. `source_type` 便于区分线上支付和线下收款。

------

# 8 消息通知域表设计

## 8.1 `notify.nt_message_template`

### 8.1.1 表用途

消息模板表，用于定义站内信、短信、企业微信等模板。

### 8.1.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 模板主键 |
| `template_code` | varchar(64) | 是 | 模板编码 |
| `template_name` | varchar(64) | 是 | 模板名称 |
| `biz_type` | varchar(32) | 是 | 业务类型 |
| `channel_type` | varchar(32) | 是 | 渠道类型 |
| `title_template` | varchar(255) | 否 | 标题模板 |
| `content_template` | text | 是 | 内容模板 |
| `status` | varchar(32) | 是 | 模板状态 |
| `created_by` | bigint | 是 | 创建人 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_by` | bigint | 是 | 更新人 |
| `updated_at` | timestamp | 是 | 更新时间 |
| `version_no` | integer | 是 | 乐观锁版本号 |
| `is_deleted` | boolean | 是 | 逻辑删除标记 |

### 8.1.3 索引与约束建议

1. `pk_nt_message_template(id)`
2. `uk_nt_template_code(template_code)`
3. `idx_nt_template_biz_channel(biz_type, channel_type, status)`

### 8.1.4 设计说明

1. 模板编码用于代码侧稳定引用；
2. 模板内容建议使用占位符方式，如 `${studentName}`；
3. 模板停用后不能再新建发送任务。

## 8.2 `notify.nt_message_task`

### 8.2.1 表用途

消息发送任务表，用于记录一次业务通知的发送主任务。

### 8.2.2 建议字段

| 字段名 | 类型 | 非空 | 说明 |
| --- | --- | --- | --- |
| `id` | bigint | 是 | 消息任务主键 |
| `task_no` | varchar(32) | 是 | 任务编号 |
| `biz_type` | varchar(32) | 是 | 业务类型 |
| `biz_id` | bigint | 是 | 业务主键 |
| `template_id` | bigint | 是 | 模板 ID |
| `channel_type` | varchar(32) | 是 | 渠道类型 |
| `request_service` | varchar(32) | 是 | 发起服务 |
| `request_no` | varchar(64) | 否 | 调用请求编号 |
| `idempotency_key` | varchar(128) | 否 | 幂等键 |
| `task_status` | varchar(32) | 是 | 任务状态 |
| `recipient_count` | integer | 否 | 接收人数 |
| `success_count` | integer | 否 | 成功数 |
| `fail_count` | integer | 否 | 失败数 |
| `requested_at` | timestamp | 否 | 请求时间 |
| `finished_at` | timestamp | 否 | 完成时间 |
| `created_at` | timestamp | 是 | 创建时间 |
| `updated_at` | timestamp | 是 | 更新时间 |

### 8.2.3 索引与约束建议

1. `pk_nt_message_task(id)`
2. `uk_nt_message_task_no(task_no)`
3. `uk_nt_message_task_idempotent(idempotency_key)`
4. `idx_nt_message_task_biz(biz_type, biz_id)`
5. `idx_nt_message_task_status(task_status, created_at)`

### 8.2.4 设计说明

1. 同一业务动作应通过 `idempotency_key` 防止重复创建发送任务；
2. 接收人明细建议拆到子表管理；
3. 消息发送结果应支持按任务维度回查。

------

# 9 关系表与日志表设计要点

## 9.1 关系表设计要点

以下关系表建议使用“组合唯一约束 + 必要审计字段”的模式：

1. `auth.sys_user_role`
2. `auth.sys_role_permission`
3. `master.md_student_guardian_rel`

建议字段：

1. `id`
2. 左侧主键
3. 右侧主键
4. `created_by`
5. `created_at`
6. `is_deleted`

## 9.2 日志表设计要点

以下日志表建议独立归档并控制索引数量：

1. 登录日志；
2. 审批动作日志；
3. 支付流水日志；
4. 消息发送日志；
5. 事件发布日志；
6. 事件消费日志；
7. 审计日志。

日志表设计建议：

1. 主键使用 bigint；
2. 时间字段必须建立查询索引；
3. 大文本字段单独控制；
4. 归档周期按月或按学期执行。

------

# 10 物理实现建议

## 10.1 PostgreSQL 类型建议

1. 主键使用 `bigint`
2. 状态与编码字段优先使用 `varchar`
3. 大文本使用 `text`
4. 金额使用 `numeric(18,2)`
5. 时间使用 `timestamp`
6. 日期使用 `date`
7. 布尔值使用 `boolean`

## 10.2 DDL 落地建议

1. 统一使用 Flyway 或 Liquibase 管理版本；
2. 每个服务维护自己的迁移脚本目录；
3. 禁止跨服务脚本互相修改 schema；
4. 所有 DDL 变更必须在测试环境完成回归。

## 10.3 数据初始化建议

基础数据初始化顺序建议如下：

1. 学校、校区、组织层级；
2. 年级、班级、学期、课程、教室；
3. 教师、学生、家长；
4. 账号与角色绑定；
5. 费用项目、审批模板、消息模板。

------

# 11 结论

本文档将学校 ERP 一期项目的核心表结构从“总体设计”进一步细化到了表级别，为后续 DDL 编写、实体类设计、Mapper 编写、接口开发、造数联调和数据迁移提供了可以直接落地的基础。后续若新增字段或新增表，应优先遵循本文档中的命名、主键、状态、快照、索引和边界规则，确保整个项目的数据模型始终保持统一和可维护。

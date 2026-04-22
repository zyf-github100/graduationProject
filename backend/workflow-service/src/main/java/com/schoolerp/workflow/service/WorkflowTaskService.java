package com.schoolerp.workflow.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.workflow.messaging.WorkflowEventPublisher;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowTaskService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final WorkflowEventPublisher workflowEventPublisher;

    public WorkflowTaskService(JdbcTemplate jdbcTemplate, WorkflowEventPublisher workflowEventPublisher) {
        this.jdbcTemplate = jdbcTemplate;
        this.workflowEventPublisher = workflowEventPublisher;
    }

    @PostConstruct
    public void initializeStorage() {
        jdbcTemplate.execute("""
                create table if not exists erp_workflow_tasks (
                    id bigint primary key,
                    process_no varchar(64) not null unique,
                    biz_type varchar(128) not null,
                    applicant_name varchar(128) not null,
                    applicant_role varchar(64) not null,
                    class_name varchar(128) not null,
                    status varchar(32) not null,
                    current_node varchar(128) not null,
                    submitted_at varchar(32) not null,
                    reason varchar(1024) not null,
                    duration varchar(256) not null
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists erp_workflow_timelines (
                    id bigserial primary key,
                    task_id bigint not null references erp_workflow_tasks(id) on delete cascade,
                    title varchar(128) not null,
                    actor varchar(128) not null,
                    event_time varchar(32) not null,
                    description varchar(1024) not null,
                    sort_order integer not null default 0
                )
                """);

        Integer count = jdbcTemplate.queryForObject("select count(*) from erp_workflow_tasks", Integer.class);
        if (count != null && count == 0) {
            seed();
        }
    }

    public List<Map<String, Object>> list(String bizType, String node, String applicant) {
        return allTasks().stream()
                .filter(task -> bizType == null || bizType.isBlank() || bizType.equals(task.get("bizType")))
                .filter(task -> node == null || node.isBlank() || task.get("currentNode").toString().contains(node))
                .filter(task -> applicant == null || applicant.isBlank()
                        || (task.get("applicantName") + " " + task.get("className")).contains(applicant))
                .sorted((left, right) -> left.get("processNo").toString().compareTo(right.get("processNo").toString()))
                .toList();
    }

    public Map<String, Object> detail(Long taskId) {
        return requireTask(taskId);
    }

    public List<Map<String, Object>> timeline(Long taskId) {
        detail(taskId);
        return jdbcTemplate.query("""
                        select title, actor, event_time, description
                        from erp_workflow_timelines
                        where task_id = ?
                        order by sort_order, id
                        """,
                (rs, rowNum) -> mapTimeline(rs),
                taskId
        );
    }

    public List<Map<String, Object>> todo(String bizType) {
        return allTasks().stream()
                .filter(task -> bizType == null || bizType.isBlank() || bizType.equals(task.get("bizType")))
                .filter(task -> !List.of("APPROVED", "REJECTED", "CANCELLED").contains(task.get("status")))
                .sorted((left, right) -> right.get("submittedAt").toString().compareTo(left.get("submittedAt").toString()))
                .map(this::toTodoItem)
                .toList();
    }

    public Map<String, Object> processInstance(Long processId) {
        Map<String, Object> task = detail(processId);
        List<Map<String, Object>> timeline = timeline(processId);

        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("processId", processId),
                Map.entry("processNo", task.get("processNo")),
                Map.entry("bizType", task.get("bizType")),
                Map.entry("status", task.get("status")),
                Map.entry("currentNode", task.get("currentNode")),
                Map.entry("applicantName", task.get("applicantName")),
                Map.entry("applicantRole", task.get("applicantRole")),
                Map.entry("className", task.get("className")),
                Map.entry("submittedAt", task.get("submittedAt")),
                Map.entry("formData", new LinkedHashMap<>(Map.ofEntries(
                        Map.entry("reason", task.get("reason")),
                        Map.entry("duration", task.get("duration"))
                ))),
                Map.entry("tasks", timeline)
        ));
    }

    @Transactional
    public Map<String, Object> approve(Long taskId, String action, String opinion, String assignee) {
        return applyAction(taskId, action, opinion, assignee);
    }

    @Transactional
    public Map<String, Object> approveLeaveRequest(Long processId, Long taskId, String comment) {
        validateProcessTask(processId, taskId);
        Map<String, Object> current = applyAction(processId, "APPROVE", comment, null);

        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("processId", processId),
                Map.entry("taskId", taskId),
                Map.entry("status", current.get("status")),
                Map.entry("nextNode", "")
        ));
    }

    @Transactional
    public Map<String, Object> rejectLeaveRequest(Long processId, Long taskId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, "驳回原因不能为空");
        }

        validateProcessTask(processId, taskId);
        Map<String, Object> current = applyAction(processId, "REJECT", comment, null);

        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("processId", processId),
                Map.entry("taskId", taskId),
                Map.entry("status", current.get("status"))
        ));
    }

    private List<Map<String, Object>> allTasks() {
        return jdbcTemplate.query("""
                        select id, process_no, biz_type, applicant_name, applicant_role, class_name,
                               status, current_node, submitted_at, reason, duration
                        from erp_workflow_tasks
                        """,
                (rs, rowNum) -> mapTask(rs)
        );
    }

    private Map<String, Object> requireTask(Long taskId) {
        List<Map<String, Object>> tasks = jdbcTemplate.query("""
                        select id, process_no, biz_type, applicant_name, applicant_role, class_name,
                               status, current_node, submitted_at, reason, duration
                        from erp_workflow_tasks
                        where id = ?
                        """,
                (rs, rowNum) -> mapTask(rs),
                taskId
        );
        if (tasks.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "审批任务不存在");
        }
        return tasks.get(0);
    }

    private Map<String, Object> mapTask(ResultSet rs) throws SQLException {
        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("id", rs.getLong("id")),
                Map.entry("processNo", rs.getString("process_no")),
                Map.entry("bizType", rs.getString("biz_type")),
                Map.entry("applicantName", rs.getString("applicant_name")),
                Map.entry("applicantRole", rs.getString("applicant_role")),
                Map.entry("className", rs.getString("class_name")),
                Map.entry("status", rs.getString("status")),
                Map.entry("currentNode", rs.getString("current_node")),
                Map.entry("submittedAt", rs.getString("submitted_at")),
                Map.entry("reason", rs.getString("reason")),
                Map.entry("duration", rs.getString("duration"))
        ));
    }

    private Map<String, Object> mapTimeline(ResultSet rs) throws SQLException {
        return Map.of(
                "title", rs.getString("title"),
                "actor", rs.getString("actor"),
                "time", rs.getString("event_time"),
                "description", rs.getString("description")
        );
    }

    private void seed() {
        insertTask(9001L, "WF-202604-0018", "学生请假", "陈思齐", "学生", "2025级软件工程3班", "APPROVING", "辅导员审批", "2026-04-16 08:50", "因流感复诊需请假两天，并已上传门诊证明。", "2026-04-16 至 2026-04-17");
        insertTask(9002L, "WF-202604-0016", "教师调课", "李老师", "任课教师", "2024级软件工程1班", "TODO", "教务管理中心审批", "2026-04-15 15:30", "因外出培训申请将周五第 2 节大学英语调整至周四第 6 节。", "单次调课");

        insertTimeline(9001L, "提交申请", "陈思齐", "2026-04-16 08:50", "已提交请假申请，并上传门诊证明 1 份。", 0);
        insertTimeline(9001L, "系统校验通过", "审批服务", "2026-04-16 08:51", "完成班级、学期和请假时间段规则校验。", 1);
        insertTimeline(9001L, "等待辅导员审批", "陈老师", "2026-04-16 08:51", "当前节点停留时长 2 小时 15 分，临近 SLA 提醒。", 2);
        insertTimeline(9002L, "提交调课申请", "李老师", "2026-04-15 15:30", "申请调整大学英语课程至周四第 6 节。", 0);
    }

    private void insertTask(Long id,
                            String processNo,
                            String bizType,
                            String applicantName,
                            String applicantRole,
                            String className,
                            String status,
                            String currentNode,
                            String submittedAt,
                            String reason,
                            String duration) {
        jdbcTemplate.update("""
                        insert into erp_workflow_tasks (
                            id, process_no, biz_type, applicant_name, applicant_role, class_name,
                            status, current_node, submitted_at, reason, duration
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                processNo,
                bizType,
                applicantName,
                applicantRole,
                className,
                status,
                currentNode,
                submittedAt,
                reason,
                duration
        );
    }

    private void insertTimeline(Long taskId, String title, String actor, String time, String description, int sortOrder) {
        jdbcTemplate.update("""
                        insert into erp_workflow_timelines (task_id, title, actor, event_time, description, sort_order)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                taskId,
                title,
                actor,
                time,
                description,
                sortOrder
        );
    }

    private void validateProcessTask(Long processId, Long taskId) {
        detail(processId);
        if (taskId == null || !processId.equals(taskId)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, "taskId 与流程不匹配");
        }
    }

    private Map<String, Object> toTodoItem(Map<String, Object> task) {
        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("taskId", task.get("id")),
                Map.entry("processId", task.get("id")),
                Map.entry("bizType", task.get("bizType")),
                Map.entry("title", task.get("applicantName") + " - " + task.get("bizType")),
                Map.entry("createdAt", task.get("submittedAt")),
                Map.entry("dueAt", task.get("submittedAt")),
                Map.entry("currentNode", task.get("currentNode")),
                Map.entry("status", task.get("status"))
        ));
    }

    private Map<String, Object> applyAction(Long taskId, String action, String opinion, String assignee) {
        Map<String, Object> current = new LinkedHashMap<>(detail(taskId));
        String upperAction = action == null ? "" : action.trim().toUpperCase();
        switch (upperAction) {
            case "APPROVE" -> {
                current.put("status", "APPROVED");
                current.put("currentNode", "流程结束");
            }
            case "REJECT" -> {
                current.put("status", "REJECTED");
                current.put("currentNode", "已驳回");
            }
            case "TRANSFER" -> current.put("currentNode", assignee == null || assignee.isBlank() ? "转交处理" : "转交 " + assignee);
            default -> throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, "action 仅支持 APPROVE、REJECT、TRANSFER");
        }

        jdbcTemplate.update("""
                        update erp_workflow_tasks
                        set status = ?,
                            current_node = ?
                        where id = ?
                        """,
                current.get("status"),
                current.get("currentNode"),
                taskId
        );
        insertTimeline(
                taskId,
                switch (upperAction) {
                    case "APPROVE" -> "审批通过";
                    case "REJECT" -> "驳回申请";
                    default -> "转交处理";
                },
                "审批服务",
                LocalDateTime.now().format(FORMATTER),
                opinion == null || opinion.isBlank() ? "已记录审批动作。" : opinion,
                nextHeadSortOrder(taskId)
        );

        workflowEventPublisher.publishTaskStatusChanged(current, upperAction, opinion);
        return current;
    }

    private int nextHeadSortOrder(Long taskId) {
        Integer minSortOrder = jdbcTemplate.queryForObject(
                "select coalesce(min(sort_order), 0) from erp_workflow_timelines where task_id = ?",
                Integer.class,
                taskId
        );
        return minSortOrder == null ? -1 : minSortOrder - 1;
    }
}

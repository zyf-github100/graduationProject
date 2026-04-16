package com.schoolerp.workflow.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.workflow.messaging.WorkflowEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowTaskService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Map<Long, Map<String, Object>> taskStore = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> timelineStore = new ConcurrentHashMap<>();
    private final WorkflowEventPublisher workflowEventPublisher;

    public WorkflowTaskService(WorkflowEventPublisher workflowEventPublisher) {
        this.workflowEventPublisher = workflowEventPublisher;
        seed();
    }

    public List<Map<String, Object>> list(String bizType, String node, String applicant) {
        return taskStore.values().stream()
                .filter(task -> bizType == null || bizType.isBlank() || bizType.equals(task.get("bizType")))
                .filter(task -> node == null || node.isBlank() || task.get("currentNode").toString().contains(node))
                .filter(task -> applicant == null || applicant.isBlank()
                        || (task.get("applicantName") + " " + task.get("className")).contains(applicant))
                .sorted((left, right) -> left.get("processNo").toString().compareTo(right.get("processNo").toString()))
                .toList();
    }

    public Map<String, Object> detail(Long taskId) {
        Map<String, Object> task = taskStore.get(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "审批任务不存在");
        }
        return task;
    }

    public List<Map<String, Object>> timeline(Long taskId) {
        detail(taskId);
        return timelineStore.getOrDefault(taskId, List.of());
    }

    public List<Map<String, Object>> todo(String bizType) {
        return taskStore.values().stream()
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

    public Map<String, Object> approve(Long taskId, String action, String opinion, String assignee) {
        return applyAction(taskId, action, opinion, assignee);
    }

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

    private void seed() {
        taskStore.put(9001L, new LinkedHashMap<>(Map.ofEntries(
                Map.entry("id", 9001L),
                Map.entry("processNo", "WF-202604-0018"),
                Map.entry("bizType", "学生请假"),
                Map.entry("applicantName", "陈思齐"),
                Map.entry("applicantRole", "学生"),
                Map.entry("className", "2025级软件工程3班"),
                Map.entry("status", "APPROVING"),
                Map.entry("currentNode", "辅导员审批"),
                Map.entry("submittedAt", "2026-04-16 08:50"),
                Map.entry("reason", "因流感复诊需请假两天，并已上传门诊证明。"),
                Map.entry("duration", "2026-04-16 至 2026-04-17")
        )));
        taskStore.put(9002L, new LinkedHashMap<>(Map.ofEntries(
                Map.entry("id", 9002L),
                Map.entry("processNo", "WF-202604-0016"),
                Map.entry("bizType", "教师调课"),
                Map.entry("applicantName", "李老师"),
                Map.entry("applicantRole", "任课教师"),
                Map.entry("className", "2024级软件工程1班"),
                Map.entry("status", "TODO"),
                Map.entry("currentNode", "教务管理中心审批"),
                Map.entry("submittedAt", "2026-04-15 15:30"),
                Map.entry("reason", "因外出培训申请将周五第 2 节大学英语调整至周四第 6 节。"),
                Map.entry("duration", "单次调课")
        )));
        timelineStore.put(9001L, List.of(
                Map.of("title", "提交申请", "actor", "陈思齐", "time", "2026-04-16 08:50", "description", "已提交请假申请，并上传门诊证明 1 份。"),
                Map.of("title", "系统校验通过", "actor", "审批服务", "time", "2026-04-16 08:51", "description", "完成班级、学期和请假时间段规则校验。"),
                Map.of("title", "等待辅导员审批", "actor", "陈老师", "time", "2026-04-16 08:51", "description", "当前节点停留时长 2 小时 15 分，临近 SLA 提醒。")
        ));
        timelineStore.put(9002L, List.of(
                Map.of("title", "提交调课申请", "actor", "李老师", "time", "2026-04-15 15:30", "description", "申请调整大学英语课程至周四第 6 节。")
        ));
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

        taskStore.put(taskId, current);
        List<Map<String, Object>> timelines = new ArrayList<>(timeline(taskId));
        timelines.add(0, Map.of(
                "title", switch (upperAction) {
                    case "APPROVE" -> "审批通过";
                    case "REJECT" -> "驳回申请";
                    default -> "转交处理";
                },
                "actor", "审批服务",
                "time", LocalDateTime.now().format(FORMATTER),
                "description", opinion == null || opinion.isBlank() ? "已记录审批动作。" : opinion
        ));
        timelineStore.put(taskId, timelines);
        workflowEventPublisher.publishTaskStatusChanged(current, upperAction, opinion);
        return current;
    }
}

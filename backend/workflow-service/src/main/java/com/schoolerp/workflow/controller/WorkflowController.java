package com.schoolerp.workflow.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.PageResult;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.workflow.service.WorkflowTaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {
    private final WorkflowTaskService workflowTaskService;

    public WorkflowController(WorkflowTaskService workflowTaskService) {
        this.workflowTaskService = workflowTaskService;
    }

    @GetMapping("/tasks")
    public ApiResponse<?> tasks(@RequestParam(defaultValue = "1") long pageNo,
                                @RequestParam(defaultValue = "10") long pageSize,
                                @RequestParam(required = false) String bizType,
                                @RequestParam(required = false) String node,
                                @RequestParam(required = false) String applicant,
                                HttpServletRequest request) {
        List<Map<String, Object>> allTasks = workflowTaskService.list(bizType, node, applicant);
        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, allTasks.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, allTasks.size());
        PageResult<Map<String, Object>> pageResult = PageResult.of(allTasks.subList(fromIndex, toIndex), pageNo, pageSize, allTasks.size());
        return ApiResponse.success(pageResult, "查询成功", requestId(request));
    }

    @GetMapping("/tasks/todo")
    public ApiResponse<?> todo(@RequestParam(defaultValue = "1") long pageNo,
                               @RequestParam(defaultValue = "10") long pageSize,
                               @RequestParam(required = false) String bizType,
                               HttpServletRequest request) {
        List<Map<String, Object>> allTasks = workflowTaskService.todo(bizType);
        int fromIndex = (int) Math.min((pageNo - 1) * pageSize, allTasks.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, allTasks.size());
        PageResult<Map<String, Object>> pageResult = PageResult.of(allTasks.subList(fromIndex, toIndex), pageNo, pageSize, allTasks.size());
        return ApiResponse.success(pageResult, "查询成功", requestId(request));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<?> detail(@PathVariable Long taskId, HttpServletRequest request) {
        return ApiResponse.success(workflowTaskService.detail(taskId), "查询成功", requestId(request));
    }

    @GetMapping("/tasks/{taskId}/timeline")
    public ApiResponse<?> timeline(@PathVariable Long taskId, HttpServletRequest request) {
        return ApiResponse.success(workflowTaskService.timeline(taskId), "查询成功", requestId(request));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public ApiResponse<?> approve(@PathVariable Long taskId,
                                  @RequestBody Map<String, String> body,
                                  HttpServletRequest request) {
        return ApiResponse.success(
                workflowTaskService.approve(
                        taskId,
                        firstNonBlank(body, "action", "decision"),
                        firstNonBlank(body, "opinion", "comment"),
                        firstNonBlank(body, "assignee", "transferTo")
                ),
                "审批处理成功",
                requestId(request)
        );
    }

    @GetMapping("/process-instances/{id}")
    public ApiResponse<?> processInstance(@PathVariable("id") Long processId, HttpServletRequest request) {
        return ApiResponse.success(workflowTaskService.processInstance(processId), "查询成功", requestId(request));
    }

    @PostMapping("/leave-requests/{id}/approve")
    public ApiResponse<?> approveLeaveRequest(@PathVariable("id") Long processId,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        Long taskId = requiredLong(body, "taskId");
        String comment = optionalString(body, "comment");
        return ApiResponse.success(
                workflowTaskService.approveLeaveRequest(processId, taskId, comment),
                "审批处理成功",
                requestId(request)
        );
    }

    @PostMapping("/leave-requests/{id}/reject")
    public ApiResponse<?> rejectLeaveRequest(@PathVariable("id") Long processId,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        Long taskId = requiredLong(body, "taskId");
        String comment = optionalString(body, "comment");
        return ApiResponse.success(
                workflowTaskService.rejectLeaveRequest(processId, taskId, comment),
                "审批处理成功",
                requestId(request)
        );
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }

    private Long requiredLong(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, key + " 不能为空");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, key + " 格式不正确");
        }
    }

    private String optionalString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(Map<String, String> body, String... keys) {
        for (String key : keys) {
            String value = body.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

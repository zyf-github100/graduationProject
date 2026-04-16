package com.schoolerp.notify.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.notify.service.NotifyTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notify")
public class NotifyController {
    private final NotifyTemplateService notifyTemplateService;

    public NotifyController(NotifyTemplateService notifyTemplateService) {
        this.notifyTemplateService = notifyTemplateService;
    }

    @GetMapping("/templates")
    public ApiResponse<?> templates(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.templates(), "查询成功", requestId(request));
    }

    @GetMapping("/student/notices")
    public ApiResponse<?> studentNotices(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.studentNotices(), "查询成功", requestId(request));
    }

    @GetMapping("/teacher/notices")
    public ApiResponse<?> teacherNotices(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.teacherNotices(), "查询成功", requestId(request));
    }

    @PostMapping("/tasks/mock-send")
    public ApiResponse<?> mockSend(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.mockSend(body), "消息任务已创建", requestId(request));
    }

    @GetMapping("/tasks/inbox")
    public ApiResponse<?> workflowInbox(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.workflowInbox(), "查询成功", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}

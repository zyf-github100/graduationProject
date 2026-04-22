package com.schoolerp.notify.controller;

import com.schoolerp.common.api.ApiResponse;
import com.schoolerp.common.web.RequestIdFilter;
import com.schoolerp.notify.dto.MessageSendRequest;
import com.schoolerp.notify.service.NotifyTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        return ApiResponse.success(notifyTemplateService.templates(), "Query successful", requestId(request));
    }

    @GetMapping("/student/notices")
    public ApiResponse<?> studentNotices(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.studentNotices(), "Query successful", requestId(request));
    }

    @PostMapping("/student/notices/{noticeId}/read")
    public ApiResponse<?> markStudentNoticeRead(@PathVariable Long noticeId, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.markStudentNoticeRead(noticeId), "Notice marked as read", requestId(request));
    }

    @GetMapping("/teacher/notices")
    public ApiResponse<?> teacherNotices(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.teacherNotices(), "Query successful", requestId(request));
    }

    @PostMapping("/tasks/mock-send")
    public ApiResponse<?> mockSend(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.mockSend(body), "Notification task created", requestId(request));
    }

    @PostMapping("/messages/send")
    public ApiResponse<?> sendMessage(@Valid @RequestBody MessageSendRequest body, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.sendMessage(body), "Notification task created", requestId(request));
    }

    @GetMapping("/messages/{taskId}")
    public ApiResponse<?> messageTask(@PathVariable String taskId, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.messageTask(taskId), "Query successful", requestId(request));
    }

    @GetMapping("/messages/{taskId}/recipients")
    public ApiResponse<?> messageRecipients(@PathVariable String taskId, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.messageRecipients(taskId), "Query successful", requestId(request));
    }

    @GetMapping("/tasks/inbox")
    public ApiResponse<?> workflowInbox(HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.workflowInbox(), "Query successful", requestId(request));
    }

    @PostMapping("/tasks/inbox/{messageId}/read")
    public ApiResponse<?> markInboxRead(@PathVariable String messageId, HttpServletRequest request) {
        return ApiResponse.success(notifyTemplateService.markInboxRead(messageId), "Inbox item marked as read", requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_unknown" : requestId.toString();
    }
}

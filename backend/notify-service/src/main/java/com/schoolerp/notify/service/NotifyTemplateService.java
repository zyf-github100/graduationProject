package com.schoolerp.notify.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.common.messaging.DomainEventMessage;
import com.schoolerp.notify.dto.MessageSendRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotifyTemplateService {
    private static final String DEMO_STUDENT_NO = "202501001";
    private static final int WORKFLOW_INBOX_LIMIT = 20;
    private static final int STUDENT_NOTICE_LIMIT = 20;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<Map<String, Object>> workflowInbox = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> studentNoticeStore = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Map<String, Object>> messageTaskStore = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> messageRecipientStore = new ConcurrentHashMap<>();
    private final AtomicLong inboxSequence = new AtomicLong(4);
    private final AtomicLong studentNoticeSequence = new AtomicLong(5);
    private final AtomicLong messageTaskSequence = new AtomicLong(1);

    public NotifyTemplateService() {
        seedStudentNotices();
        seedWorkflowInbox();
    }

    public List<Map<String, Object>> templates() {
        return List.of(
                Map.of("templateCode", "WORKFLOW_APPROVE_NOTICE", "templateName", "Workflow status notification", "channel", "INTERNAL_MESSAGE", "status", "ENABLED"),
                Map.of("templateCode", "BILL_REMINDER", "templateName", "Billing reminder", "channel", "INTERNAL_MESSAGE", "status", "ENABLED"),
                Map.of("templateCode", "GRADE_PUBLISH_NOTICE", "templateName", "Grade publish reminder", "channel", "WECHAT", "status", "DRAFT")
        );
    }

    public Map<String, Object> studentNotices() {
        List<Map<String, Object>> records = snapshot(studentNoticeStore);
        return Map.of(
                "records", records,
                "unreadCount", records.stream().filter(notice -> Boolean.FALSE.equals(notice.get("isRead"))).count(),
                "categorySummary", buildCategorySummary(records),
                "timeline", List.of(
                        Map.of("time", "Today 08:00", "content", "Completed morning attendance", "actor", "Student portal"),
                        Map.of("time", "Today 11:45", "content", "Java experiment report submitted", "actor", "Lab platform"),
                        Map.of("time", "Today 15:30", "content", "Innovation week notice confirmed", "actor", "Campus app")
                )
        );
    }

    public Map<String, Object> teacherNotices() {
        List<Map<String, Object>> records = teacherNoticeRecords();
        return Map.of(
                "records", records,
                "unreadCount", records.stream().filter(notice -> Boolean.FALSE.equals(notice.get("isRead"))).count(),
                "categorySummary", buildCategorySummary(records)
        );
    }

    public Map<String, Object> mockSend(Map<String, Object> payload) {
        Map<String, Object> createdItem = createInboxItem(
                nextInboxId(),
                stringValue(payload.get("title"), "Manual notification task"),
                stringValue(payload.get("summary"), "A notification task was queued manually."),
                stringValue(payload.get("bizType"), "Manual Task"),
                stringValue(payload.get("bizId"), "NT-202604-0001"),
                stringValue(payload.get("eventType"), "ManualNotifyTaskCreated"),
                stringValue(payload.get("sourceService"), "notify-service"),
                stringValue(payload.get("routingKey"), "notify.manual"),
                stringValue(payload.get("occurredAt"), formatOccurredAt(OffsetDateTime.now())),
                stringValue(payload.get("workflowStatus"), "QUEUED"),
                stringValue(payload.get("priority"), "normal"),
                false
        );

        prependInboxItem(createdItem);
        return new LinkedHashMap<>(Map.of(
                "taskNo", createdItem.get("bizId"),
                "status", createdItem.get("workflowStatus"),
                "payload", payload,
                "inboxItem", createdItem
        ));
    }

    public Map<String, Object> sendMessage(MessageSendRequest request) {
        String taskId = nextMessageTaskId();
        String createdAt = formatOccurredAt(OffsetDateTime.now());
        String bizType = stringValue(request.bizType(), "账单催缴");
        String bizId = stringValue(request.bizId(), taskId);

        List<Map<String, Object>> recipients = new ArrayList<>();
        int index = 1;
        for (MessageSendRequest.Recipient recipient : request.recipients()) {
            recipients.add(createRecipientRecord(taskId, request.channel(), createdAt, recipient, index));
            if (DEMO_STUDENT_NO.equals(recipient.studentNo())) {
                prependStudentNotice(createBillingStudentNotice(recipient, createdAt));
            }
            index++;
        }

        messageRecipientStore.put(taskId, recipients);

        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("taskNo", taskId);
        task.put("templateCode", request.templateCode());
        task.put("title", request.title());
        task.put("summary", request.summary());
        task.put("channel", request.channel());
        task.put("bizType", bizType);
        task.put("bizId", bizId);
        task.put("status", "SENT");
        task.put("recipientCount", recipients.size());
        task.put("successCount", recipients.size());
        task.put("failedCount", 0);
        task.put("createdAt", createdAt);
        task.put("updatedAt", createdAt);
        messageTaskStore.put(taskId, task);

        prependInboxItem(createInboxItem(
                nextInboxId(),
                request.title(),
                request.summary(),
                bizType,
                bizId,
                "NotifyMessageSent",
                "notify-service",
                "notify.message.send",
                createdAt,
                "SENT",
                recipients.size() >= 5 ? "high" : "normal",
                false
        ));

        return new LinkedHashMap<>(task);
    }

    public Map<String, Object> messageTask(String taskId) {
        return new LinkedHashMap<>(requireMessageTask(taskId));
    }

    public List<Map<String, Object>> messageRecipients(String taskId) {
        requireMessageTask(taskId);
        return snapshot(messageRecipientStore.getOrDefault(taskId, List.of()));
    }

    public void acceptWorkflowEvent(DomainEventMessage message) {
        Map<String, Object> payload = message.payload() == null ? Map.of() : message.payload();
        String workflowStatus = stringValue(payload.get("status"), "INFO");
        String action = stringValue(payload.get("action"), "update");
        String applicantName = stringValue(payload.get("applicantName"), "Unknown applicant");
        String className = stringValue(payload.get("className"), "Unknown class");
        String title = stringValue(message.title(), "Workflow status updated");
        String summary = stringValue(
                message.detail(),
                applicantName + " / " + className + " / " + workflowStatus
        );

        prependInboxItem(createInboxItem(
                nextInboxId(),
                title,
                summary,
                stringValue(message.bizType(), "Workflow"),
                stringValue(message.bizId(), ""),
                stringValue(message.eventType(), "WorkflowTaskStatusChanged"),
                stringValue(message.sourceService(), "workflow-service"),
                stringValue(message.routingKey(), "workflow.update"),
                message.occurredAt() == null ? formatOccurredAt(OffsetDateTime.now()) : formatOccurredAt(message.occurredAt()),
                workflowStatus,
                resolvePriority(workflowStatus, action),
                false
        ));
    }

    public List<Map<String, Object>> workflowInbox() {
        return snapshot(workflowInbox);
    }

    public Map<String, Object> markInboxRead(String messageId) {
        synchronized (workflowInbox) {
            for (Map<String, Object> item : workflowInbox) {
                if (messageId.equals(item.get("id"))) {
                    item.put("isRead", true);
                    return new LinkedHashMap<>(item);
                }
            }
        }
        throw new BusinessException(ResultCode.NOT_FOUND, 404, "Inbox item not found");
    }

    private List<Map<String, Object>> teacherNoticeRecords() {
        return List.of(
                Map.of("id", 101, "title", "Midterm invigilation schedule released", "category", "Academic Notice", "publisher", "Academic affairs office", "publishTime", "2026-04-17 08:20", "isRead", false, "summary", "Please confirm your invigilation slot before 17:00 today. Submit a change request if there is a conflict.", "priority", "high"),
                Map.of("id", 102, "title", "Course group weekly meeting reminder", "category", "Course Group Notice", "publisher", "Basic teaching department", "publishTime", "2026-04-16 15:30", "isRead", false, "summary", "This week's meeting focuses on exam quality and assignment review rhythm. Please prepare feedback.", "priority", "normal"),
                Map.of("id", 103, "title", "Teacher training sign-in rule adjusted", "category", "Administrative Notice", "publisher", "Information service center", "publishTime", "2026-04-15 11:00", "isRead", true, "summary", "Teacher training sign-in now requires employee ID plus a dynamic code. Check your account binding first.", "priority", "normal")
        );
    }

    private void seedStudentNotices() {
        prependStudentNotice(new LinkedHashMap<>(Map.of(
                "id", 4L,
                "title", "Library study room rules updated",
                "category", "Campus Bulletin",
                "publisher", "Library",
                "publishTime", "2026-04-14 10:30",
                "isRead", true,
                "summary", "Night study room reservations now support extended slots and updated no-show handling.",
                "priority", "normal"
        )));
        prependStudentNotice(new LinkedHashMap<>(Map.of(
                "id", 3L,
                "title", "Innovation week registration started",
                "category", "Campus Activity",
                "publisher", "Student development center",
                "publishTime", "2026-04-15 14:00",
                "isRead", true,
                "summary", "Innovation week registration stays open until Friday 17:00 through the student service platform.",
                "priority", "normal"
        )));
        prependStudentNotice(new LinkedHashMap<>(Map.of(
                "id", 2L,
                "title", "Dormitory fee reminder",
                "category", "Billing Notice",
                "publisher", "Finance center",
                "publishTime", "2026-04-16 16:40",
                "isRead", false,
                "summary", "Dormitory fees are due before 2026-04-25. Please complete payment or submit a delay request.",
                "priority", "high"
        )));
        prependStudentNotice(new LinkedHashMap<>(Map.of(
                "id", 1L,
                "title", "Midterm exam arrangement released",
                "category", "Teaching Notice",
                "publisher", "Academic affairs office",
                "publishTime", "2026-04-17 09:20",
                "isRead", false,
                "summary", "Midterm exams will run from Wednesday to Friday next week. Please arrive 15 minutes early.",
                "priority", "high"
        )));
    }

    private void seedWorkflowInbox() {
        prependInboxItem(createInboxItem(
                "INBOX-202604-0003",
                "Billing reminder dispatched",
                "The April dormitory payment reminder has been queued for 186 students.",
                "Billing Reminder",
                "BILL-202604-0142",
                "BillingReminderCreated",
                "notify-service",
                "notify.billing",
                "2026-04-17 09:45",
                "QUEUED",
                "normal",
                true
        ));
        prependInboxItem(createInboxItem(
                "INBOX-202604-0002",
                "Teacher schedule adjustment was rejected",
                "WF-202604-0016 was rejected after timetable conflict validation.",
                "Teacher Schedule Change",
                "WF-202604-0016",
                "WorkflowTaskStatusChanged",
                "workflow-service",
                "workflow.reject",
                "2026-04-17 09:10",
                "REJECTED",
                "high",
                false
        ));
        prependInboxItem(createInboxItem(
                "INBOX-202604-0001",
                "Student leave request is pending review",
                "WF-202604-0018 is waiting for counselor confirmation before class starts.",
                "Student Leave",
                "WF-202604-0018",
                "WorkflowTaskStatusChanged",
                "workflow-service",
                "workflow.approve",
                "2026-04-17 08:30",
                "APPROVING",
                "high",
                false
        ));
    }

    private List<Map<String, Object>> buildCategorySummary(List<Map<String, Object>> records) {
        Map<String, int[]> summary = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            String category = stringValue(record.get("category"), "Other");
            int[] bucket = summary.computeIfAbsent(category, ignored -> new int[]{0, 0});
            bucket[0]++;
            if (Boolean.FALSE.equals(record.get("isRead"))) {
                bucket[1]++;
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : summary.entrySet()) {
            int total = entry.getValue()[0];
            int unread = entry.getValue()[1];
            result.add(Map.of(
                    "category", entry.getKey(),
                    "label", unread + " 条未读 / 共 " + total + " 条"
            ));
        }
        return result;
    }

    private Map<String, Object> createRecipientRecord(String taskId,
                                                      String channel,
                                                      String sentAt,
                                                      MessageSendRequest.Recipient recipient,
                                                      int index) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recipientId", taskId + "-" + String.format("%03d", index));
        item.put("taskId", taskId);
        item.put("billId", recipient.billId());
        item.put("billNo", recipient.billNo());
        item.put("studentNo", recipient.studentNo());
        item.put("recipientName", recipient.studentName());
        item.put("className", recipient.className());
        item.put("feeItemName", recipient.feeItemName());
        item.put("outstandingAmount", recipient.outstandingAmount());
        item.put("dueDate", recipient.dueDate());
        item.put("channel", channel);
        item.put("status", "DELIVERED");
        item.put("sentAt", sentAt);
        return item;
    }

    private Map<String, Object> createBillingStudentNotice(MessageSendRequest.Recipient recipient, String publishTime) {
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("id", studentNoticeSequence.getAndIncrement());
        notice.put("referenceNo", recipient.billNo());
        notice.put("title", "缴费提醒：" + recipient.feeItemName());
        notice.put("category", "Billing Notice");
        notice.put("publisher", "财务中心");
        notice.put("publishTime", publishTime);
        notice.put("isRead", false);
        notice.put("summary", "账单 " + recipient.billNo() + " 仍有 " + recipient.outstandingAmount() + " 元待缴，请在 " + recipient.dueDate() + " 前完成缴费。");
        notice.put("priority", isUrgentDueDate(recipient.dueDate()) ? "high" : "normal");
        return notice;
    }

    private boolean isUrgentDueDate(String dueDate) {
        LocalDate current = LocalDate.now();
        LocalDate date = LocalDate.parse(dueDate, DATE_FORMATTER);
        return !date.isAfter(current.plusDays(3));
    }

    private void prependStudentNotice(Map<String, Object> notice) {
        synchronized (studentNoticeStore) {
            Object referenceNo = notice.get("referenceNo");
            if (referenceNo != null) {
                studentNoticeStore.removeIf(item -> referenceNo.equals(item.get("referenceNo")));
            }
            studentNoticeStore.add(0, notice);
            while (studentNoticeStore.size() > STUDENT_NOTICE_LIMIT) {
                studentNoticeStore.remove(studentNoticeStore.size() - 1);
            }
        }
    }

    private void prependInboxItem(Map<String, Object> item) {
        synchronized (workflowInbox) {
            workflowInbox.add(0, item);
            while (workflowInbox.size() > WORKFLOW_INBOX_LIMIT) {
                workflowInbox.remove(workflowInbox.size() - 1);
            }
        }
    }

    private Map<String, Object> createInboxItem(String id,
                                                String title,
                                                String summary,
                                                String bizType,
                                                String bizId,
                                                String eventType,
                                                String sourceService,
                                                String routingKey,
                                                String occurredAt,
                                                String workflowStatus,
                                                String priority,
                                                boolean isRead) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("title", title);
        item.put("summary", summary);
        item.put("bizType", bizType);
        item.put("bizId", bizId);
        item.put("eventType", eventType);
        item.put("sourceService", sourceService);
        item.put("routingKey", routingKey);
        item.put("occurredAt", occurredAt);
        item.put("workflowStatus", workflowStatus);
        item.put("priority", priority);
        item.put("isRead", isRead);
        return item;
    }

    private Map<String, Object> requireMessageTask(String taskId) {
        Map<String, Object> task = messageTaskStore.get(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Notification task not found");
        }
        return task;
    }

    private List<Map<String, Object>> snapshot(List<Map<String, Object>> source) {
        synchronized (source) {
            List<Map<String, Object>> snapshot = new ArrayList<>(source.size());
            for (Map<String, Object> item : source) {
                Map<String, Object> copy = new LinkedHashMap<>(item);
                copy.remove("referenceNo");
                snapshot.add(copy);
            }
            return snapshot;
        }
    }

    private String nextInboxId() {
        return "INBOX-202604-" + String.format("%04d", inboxSequence.getAndIncrement());
    }

    private String nextMessageTaskId() {
        return "MSG-202604-" + String.format("%04d", messageTaskSequence.getAndIncrement());
    }

    private String resolvePriority(String workflowStatus, String action) {
        if ("REJECTED".equalsIgnoreCase(workflowStatus) || "reject".equalsIgnoreCase(action)) {
            return "high";
        }
        if ("APPROVING".equalsIgnoreCase(workflowStatus) || "transfer".equalsIgnoreCase(action)) {
            return "high";
        }
        return "normal";
    }

    private String formatOccurredAt(OffsetDateTime occurredAt) {
        return occurredAt.toLocalDateTime().format(DISPLAY_TIME_FORMATTER);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String stringValue = value.toString();
        return stringValue.isBlank() ? fallback : stringValue;
    }
}

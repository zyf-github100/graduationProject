package com.schoolerp.notify.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.common.messaging.DomainEventMessage;
import com.schoolerp.notify.dto.MessageSendRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotifyTemplateService {
    private static final String DEMO_STUDENT_NO = "202501001";
    private static final int WORKFLOW_INBOX_LIMIT = 20;
    private static final int STUDENT_NOTICE_LIMIT = 20;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter NUMBER_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final JdbcTemplate jdbcTemplate;

    public NotifyTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeStorage() {
        jdbcTemplate.execute("""
                create table if not exists erp_student_notices (
                    id bigint primary key,
                    title varchar(256) not null,
                    category varchar(128) not null,
                    publisher varchar(128) not null,
                    publish_time varchar(32) not null,
                    is_read boolean not null default false,
                    summary varchar(1024) not null,
                    priority varchar(32) not null,
                    reference_no varchar(128)
                )
                """);
        jdbcTemplate.execute("""
                create unique index if not exists uk_erp_student_notices_reference
                on erp_student_notices(reference_no)
                where reference_no is not null
                """);
        jdbcTemplate.execute("""
                create table if not exists erp_workflow_inbox (
                    id varchar(64) primary key,
                    title varchar(256) not null,
                    summary varchar(1024) not null,
                    biz_type varchar(128) not null,
                    biz_id varchar(128) not null,
                    event_type varchar(128) not null,
                    source_service varchar(128) not null,
                    routing_key varchar(128) not null,
                    occurred_at varchar(32) not null,
                    workflow_status varchar(64) not null,
                    priority varchar(32) not null,
                    is_read boolean not null default false
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists erp_notify_message_tasks (
                    task_id varchar(64) primary key,
                    task_no varchar(64) not null,
                    template_code varchar(128) not null,
                    title varchar(256) not null,
                    summary varchar(1024) not null,
                    channel varchar(64) not null,
                    biz_type varchar(128) not null,
                    biz_id varchar(128) not null,
                    status varchar(32) not null,
                    recipient_count integer not null,
                    success_count integer not null,
                    failed_count integer not null,
                    created_at varchar(32) not null,
                    updated_at varchar(32) not null
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists erp_notify_message_recipients (
                    recipient_id varchar(96) primary key,
                    task_id varchar(64) not null references erp_notify_message_tasks(task_id) on delete cascade,
                    bill_id bigint not null,
                    bill_no varchar(64) not null,
                    student_no varchar(64) not null,
                    recipient_name varchar(128) not null,
                    class_name varchar(128) not null,
                    fee_item_name varchar(128) not null,
                    outstanding_amount integer not null,
                    due_date varchar(32) not null,
                    channel varchar(64) not null,
                    status varchar(32) not null,
                    sent_at varchar(32) not null
                )
                """);

        seedStudentNoticesIfEmpty();
        seedWorkflowInboxIfEmpty();
    }

    public List<Map<String, Object>> templates() {
        return List.of(
                Map.of("templateCode", "WORKFLOW_APPROVE_NOTICE", "templateName", "Workflow status notification", "channel", "INTERNAL_MESSAGE", "status", "ENABLED"),
                Map.of("templateCode", "BILL_REMINDER", "templateName", "Billing reminder", "channel", "INTERNAL_MESSAGE", "status", "ENABLED"),
                Map.of("templateCode", "GRADE_PUBLISH_NOTICE", "templateName", "Grade publish reminder", "channel", "WECHAT", "status", "DRAFT")
        );
    }

    public Map<String, Object> studentNotices() {
        List<Map<String, Object>> records = studentNoticeRecords();
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

    @Transactional
    public Map<String, Object> markStudentNoticeRead(Long noticeId) {
        int updated = jdbcTemplate.update("update erp_student_notices set is_read = true where id = ?", noticeId);
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Student notice not found");
        }
        return sanitizedCopy(requireStudentNotice(noticeId));
    }

    public Map<String, Object> teacherNotices() {
        List<Map<String, Object>> records = teacherNoticeRecords();
        return Map.of(
                "records", records,
                "unreadCount", records.stream().filter(notice -> Boolean.FALSE.equals(notice.get("isRead"))).count(),
                "categorySummary", buildCategorySummary(records)
        );
    }

    @Transactional
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

    @Transactional
    public Map<String, Object> sendMessage(MessageSendRequest request) {
        String taskId = nextMessageTaskId();
        String createdAt = formatOccurredAt(OffsetDateTime.now());
        String bizType = stringValue(request.bizType(), "账单催缴");
        String bizId = stringValue(request.bizId(), taskId);

        List<Map<String, Object>> recipients = new ArrayList<>();
        int index = 1;
        for (MessageSendRequest.Recipient recipient : request.recipients()) {
            recipients.add(createRecipientRecord(taskId, request.channel(), createdAt, recipient, index));
            index++;
        }

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
        insertMessageTask(task);

        for (Map<String, Object> recipientRecord : recipients) {
            insertMessageRecipient(recipientRecord);
        }
        for (MessageSendRequest.Recipient recipient : request.recipients()) {
            if (DEMO_STUDENT_NO.equals(recipient.studentNo())) {
                prependStudentNotice(createBillingStudentNotice(recipient, createdAt));
            }
        }

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
        return jdbcTemplate.query("""
                        select recipient_id, task_id, bill_id, bill_no, student_no, recipient_name,
                               class_name, fee_item_name, outstanding_amount, due_date, channel, status, sent_at
                        from erp_notify_message_recipients
                        where task_id = ?
                        order by recipient_id
                        """,
                (rs, rowNum) -> mapMessageRecipient(rs),
                taskId
        );
    }

    @Transactional
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
        return jdbcTemplate.query("""
                        select id, title, summary, biz_type, biz_id, event_type, source_service, routing_key,
                               occurred_at, workflow_status, priority, is_read
                        from erp_workflow_inbox
                        order by occurred_at desc, id desc
                        limit ?
                        """,
                (rs, rowNum) -> mapInboxItem(rs),
                WORKFLOW_INBOX_LIMIT
        );
    }

    @Transactional
    public Map<String, Object> markInboxRead(String messageId) {
        int updated = jdbcTemplate.update("update erp_workflow_inbox set is_read = true where id = ?", messageId);
        if (updated == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Inbox item not found");
        }
        return requireInboxItem(messageId);
    }

    private List<Map<String, Object>> teacherNoticeRecords() {
        return List.of(
                Map.of("id", 101, "title", "Midterm invigilation schedule released", "category", "Academic Notice", "publisher", "Academic affairs office", "publishTime", "2026-04-17 08:20", "isRead", false, "summary", "Please confirm your invigilation slot before 17:00 today. Submit a change request if there is a conflict.", "priority", "high"),
                Map.of("id", 102, "title", "Course group weekly meeting reminder", "category", "Course Group Notice", "publisher", "Basic teaching department", "publishTime", "2026-04-16 15:30", "isRead", false, "summary", "This week's meeting focuses on exam quality and assignment review rhythm. Please prepare feedback.", "priority", "normal"),
                Map.of("id", 103, "title", "Teacher training sign-in rule adjusted", "category", "Administrative Notice", "publisher", "Information service center", "publishTime", "2026-04-15 11:00", "isRead", true, "summary", "Teacher training sign-in now requires employee ID plus a dynamic code. Check your account binding first.", "priority", "normal")
        );
    }

    private void seedStudentNoticesIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from erp_student_notices", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        insertStudentNotice(studentNotice(
                4L,
                "Library study room rules updated",
                "Campus Bulletin",
                "Library",
                "2026-04-14 10:30",
                true,
                "Night study room reservations now support extended slots and updated no-show handling.",
                "normal",
                null
        ));
        insertStudentNotice(studentNotice(
                3L,
                "Innovation week registration started",
                "Campus Activity",
                "Student development center",
                "2026-04-15 14:00",
                true,
                "Innovation week registration stays open until Friday 17:00 through the student service platform.",
                "normal",
                null
        ));
        insertStudentNotice(studentNotice(
                2L,
                "Dormitory fee reminder",
                "Billing Notice",
                "Finance center",
                "2026-04-16 16:40",
                false,
                "Dormitory fees are due before 2026-04-25. Please complete payment or submit a delay request.",
                "high",
                null
        ));
        insertStudentNotice(studentNotice(
                1L,
                "Midterm exam arrangement released",
                "Teaching Notice",
                "Academic affairs office",
                "2026-04-17 09:20",
                false,
                "Midterm exams will run from Wednesday to Friday next week. Please arrive 15 minutes early.",
                "high",
                null
        ));
    }

    private void seedWorkflowInboxIfEmpty() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from erp_workflow_inbox", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        insertInboxItem(createInboxItem(
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
        insertInboxItem(createInboxItem(
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
        insertInboxItem(createInboxItem(
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
        notice.put("id", nextStudentNoticeId());
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
        Object referenceNo = notice.get("referenceNo");
        if (referenceNo != null) {
            jdbcTemplate.update("delete from erp_student_notices where reference_no = ?", referenceNo);
        }
        insertStudentNotice(notice);
        trimStudentNotices();
    }

    private void prependInboxItem(Map<String, Object> item) {
        insertInboxItem(item);
        trimWorkflowInbox();
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

    private List<Map<String, Object>> studentNoticeRecords() {
        return jdbcTemplate.query("""
                        select id, title, category, publisher, publish_time, is_read, summary, priority, reference_no
                        from erp_student_notices
                        order by publish_time desc, id desc
                        limit ?
                        """,
                (rs, rowNum) -> sanitizedCopy(mapStudentNotice(rs)),
                STUDENT_NOTICE_LIMIT
        );
    }

    private Map<String, Object> requireStudentNotice(Long noticeId) {
        List<Map<String, Object>> records = jdbcTemplate.query("""
                        select id, title, category, publisher, publish_time, is_read, summary, priority, reference_no
                        from erp_student_notices
                        where id = ?
                        """,
                (rs, rowNum) -> mapStudentNotice(rs),
                noticeId
        );
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Student notice not found");
        }
        return records.get(0);
    }

    private Map<String, Object> requireInboxItem(String messageId) {
        List<Map<String, Object>> records = jdbcTemplate.query("""
                        select id, title, summary, biz_type, biz_id, event_type, source_service, routing_key,
                               occurred_at, workflow_status, priority, is_read
                        from erp_workflow_inbox
                        where id = ?
                        """,
                (rs, rowNum) -> mapInboxItem(rs),
                messageId
        );
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Inbox item not found");
        }
        return records.get(0);
    }

    private Map<String, Object> requireMessageTask(String taskId) {
        List<Map<String, Object>> records = jdbcTemplate.query("""
                        select task_id, task_no, template_code, title, summary, channel, biz_type, biz_id,
                               status, recipient_count, success_count, failed_count, created_at, updated_at
                        from erp_notify_message_tasks
                        where task_id = ?
                        """,
                (rs, rowNum) -> mapMessageTask(rs),
                taskId
        );
        if (records.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "Notification task not found");
        }
        return records.get(0);
    }

    private void insertStudentNotice(Map<String, Object> notice) {
        jdbcTemplate.update("""
                        insert into erp_student_notices (
                            id, title, category, publisher, publish_time, is_read, summary, priority, reference_no
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                notice.get("id"),
                notice.get("title"),
                notice.get("category"),
                notice.get("publisher"),
                notice.get("publishTime"),
                notice.get("isRead"),
                notice.get("summary"),
                notice.get("priority"),
                notice.get("referenceNo")
        );
    }

    private void insertInboxItem(Map<String, Object> item) {
        jdbcTemplate.update("""
                        insert into erp_workflow_inbox (
                            id, title, summary, biz_type, biz_id, event_type, source_service, routing_key,
                            occurred_at, workflow_status, priority, is_read
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                item.get("id"),
                item.get("title"),
                item.get("summary"),
                item.get("bizType"),
                item.get("bizId"),
                item.get("eventType"),
                item.get("sourceService"),
                item.get("routingKey"),
                item.get("occurredAt"),
                item.get("workflowStatus"),
                item.get("priority"),
                item.get("isRead")
        );
    }

    private void insertMessageTask(Map<String, Object> task) {
        jdbcTemplate.update("""
                        insert into erp_notify_message_tasks (
                            task_id, task_no, template_code, title, summary, channel, biz_type, biz_id,
                            status, recipient_count, success_count, failed_count, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                task.get("taskId"),
                task.get("taskNo"),
                task.get("templateCode"),
                task.get("title"),
                task.get("summary"),
                task.get("channel"),
                task.get("bizType"),
                task.get("bizId"),
                task.get("status"),
                task.get("recipientCount"),
                task.get("successCount"),
                task.get("failedCount"),
                task.get("createdAt"),
                task.get("updatedAt")
        );
    }

    private void insertMessageRecipient(Map<String, Object> recipient) {
        jdbcTemplate.update("""
                        insert into erp_notify_message_recipients (
                            recipient_id, task_id, bill_id, bill_no, student_no, recipient_name,
                            class_name, fee_item_name, outstanding_amount, due_date, channel, status, sent_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                recipient.get("recipientId"),
                recipient.get("taskId"),
                recipient.get("billId"),
                recipient.get("billNo"),
                recipient.get("studentNo"),
                recipient.get("recipientName"),
                recipient.get("className"),
                recipient.get("feeItemName"),
                recipient.get("outstandingAmount"),
                recipient.get("dueDate"),
                recipient.get("channel"),
                recipient.get("status"),
                recipient.get("sentAt")
        );
    }

    private Map<String, Object> mapStudentNotice(ResultSet rs) throws SQLException {
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("id", rs.getLong("id"));
        notice.put("title", rs.getString("title"));
        notice.put("category", rs.getString("category"));
        notice.put("publisher", rs.getString("publisher"));
        notice.put("publishTime", rs.getString("publish_time"));
        notice.put("isRead", rs.getBoolean("is_read"));
        notice.put("summary", rs.getString("summary"));
        notice.put("priority", rs.getString("priority"));
        notice.put("referenceNo", rs.getString("reference_no"));
        return notice;
    }

    private Map<String, Object> mapInboxItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getString("id"));
        item.put("title", rs.getString("title"));
        item.put("summary", rs.getString("summary"));
        item.put("bizType", rs.getString("biz_type"));
        item.put("bizId", rs.getString("biz_id"));
        item.put("eventType", rs.getString("event_type"));
        item.put("sourceService", rs.getString("source_service"));
        item.put("routingKey", rs.getString("routing_key"));
        item.put("occurredAt", rs.getString("occurred_at"));
        item.put("workflowStatus", rs.getString("workflow_status"));
        item.put("priority", rs.getString("priority"));
        item.put("isRead", rs.getBoolean("is_read"));
        return item;
    }

    private Map<String, Object> mapMessageTask(ResultSet rs) throws SQLException {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", rs.getString("task_id"));
        task.put("taskNo", rs.getString("task_no"));
        task.put("templateCode", rs.getString("template_code"));
        task.put("title", rs.getString("title"));
        task.put("summary", rs.getString("summary"));
        task.put("channel", rs.getString("channel"));
        task.put("bizType", rs.getString("biz_type"));
        task.put("bizId", rs.getString("biz_id"));
        task.put("status", rs.getString("status"));
        task.put("recipientCount", rs.getInt("recipient_count"));
        task.put("successCount", rs.getInt("success_count"));
        task.put("failedCount", rs.getInt("failed_count"));
        task.put("createdAt", rs.getString("created_at"));
        task.put("updatedAt", rs.getString("updated_at"));
        return task;
    }

    private Map<String, Object> mapMessageRecipient(ResultSet rs) throws SQLException {
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("recipientId", rs.getString("recipient_id"));
        recipient.put("taskId", rs.getString("task_id"));
        recipient.put("billId", rs.getLong("bill_id"));
        recipient.put("billNo", rs.getString("bill_no"));
        recipient.put("studentNo", rs.getString("student_no"));
        recipient.put("recipientName", rs.getString("recipient_name"));
        recipient.put("className", rs.getString("class_name"));
        recipient.put("feeItemName", rs.getString("fee_item_name"));
        recipient.put("outstandingAmount", rs.getInt("outstanding_amount"));
        recipient.put("dueDate", rs.getString("due_date"));
        recipient.put("channel", rs.getString("channel"));
        recipient.put("status", rs.getString("status"));
        recipient.put("sentAt", rs.getString("sent_at"));
        return recipient;
    }

    private Map<String, Object> studentNotice(Long id,
                                              String title,
                                              String category,
                                              String publisher,
                                              String publishTime,
                                              boolean isRead,
                                              String summary,
                                              String priority,
                                              String referenceNo) {
        Map<String, Object> notice = new LinkedHashMap<>();
        notice.put("id", id);
        notice.put("title", title);
        notice.put("category", category);
        notice.put("publisher", publisher);
        notice.put("publishTime", publishTime);
        notice.put("isRead", isRead);
        notice.put("summary", summary);
        notice.put("priority", priority);
        notice.put("referenceNo", referenceNo);
        return notice;
    }

    private void trimStudentNotices() {
        jdbcTemplate.update("""
                delete from erp_student_notices
                where id in (
                    select id from (
                        select id from erp_student_notices
                        order by publish_time desc, id desc
                        offset ?
                    ) expired
                )
                """, STUDENT_NOTICE_LIMIT);
    }

    private void trimWorkflowInbox() {
        jdbcTemplate.update("""
                delete from erp_workflow_inbox
                where id in (
                    select id from (
                        select id from erp_workflow_inbox
                        order by occurred_at desc, id desc
                        offset ?
                    ) expired
                )
                """, WORKFLOW_INBOX_LIMIT);
    }

    private Map<String, Object> sanitizedCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove("referenceNo");
        return copy;
    }

    private Long nextStudentNoticeId() {
        Number value = jdbcTemplate.queryForObject("select coalesce(max(id), 0) + 1 from erp_student_notices", Number.class);
        return value == null ? 1L : value.longValue();
    }

    private String nextInboxId() {
        String prefix = "INBOX-" + LocalDate.now().format(NUMBER_MONTH_FORMATTER) + "-";
        return prefix + String.format("%04d", nextSuffixedNumber("erp_workflow_inbox", "id", prefix));
    }

    private String nextMessageTaskId() {
        String prefix = "MSG-" + LocalDate.now().format(NUMBER_MONTH_FORMATTER) + "-";
        return prefix + String.format("%04d", nextSuffixedNumber("erp_notify_message_tasks", "task_id", prefix));
    }

    private long nextSuffixedNumber(String tableName, String columnName, String prefix) {
        String sql = "select coalesce(max(cast(substring(" + columnName + " from " + (prefix.length() + 1) + ") as integer)), 0) + 1 "
                + "from " + tableName + " where " + columnName + " like ?";
        Number value = jdbcTemplate.queryForObject(sql, Number.class, prefix + "%");
        return value == null ? 1L : value.longValue();
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

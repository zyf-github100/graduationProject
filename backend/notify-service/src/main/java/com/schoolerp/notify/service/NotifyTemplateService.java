package com.schoolerp.notify.service;

import com.schoolerp.common.messaging.DomainEventMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class NotifyTemplateService {
    private final Deque<Map<String, Object>> workflowInbox = new ConcurrentLinkedDeque<>();

    public List<Map<String, Object>> templates() {
        return List.of(
                Map.of("templateCode", "WORKFLOW_APPROVE_NOTICE", "templateName", "审批结果通知", "channel", "INTERNAL_MESSAGE", "status", "ENABLED"),
                Map.of("templateCode", "BILL_REMINDER", "templateName", "账单催缴提醒", "channel", "SMS", "status", "ENABLED"),
                Map.of("templateCode", "GRADE_PUBLISH_NOTICE", "templateName", "成绩发布提醒", "channel", "WECHAT", "status", "DRAFT")
        );
    }

    public Map<String, Object> studentNotices() {
        List<Map<String, Object>> records = studentNoticeRecords();
        return Map.of(
                "records", records,
                "unreadCount", records.stream().filter(notice -> Boolean.FALSE.equals(notice.get("isRead"))).count(),
                "categorySummary", List.of(
                        Map.of("category", "教学通知", "label", "1 条未读"),
                        Map.of("category", "财务通知", "label", "1 条未读"),
                        Map.of("category", "校园活动", "label", "1 条已读"),
                        Map.of("category", "校园公告", "label", "1 条已读")
                ),
                "timeline", List.of(
                        Map.of("time", "今天 08:00", "content", "完成课程签到", "actor", "学习事务系统"),
                        Map.of("time", "今天 11:45", "content", "Java 实验报告已提交", "actor", "实验平台"),
                        Map.of("time", "今天 15:30", "content", "创新实践周通知已确认", "actor", "第二课堂平台")
                )
        );
    }

    public Map<String, Object> teacherNotices() {
        List<Map<String, Object>> records = teacherNoticeRecords();
        return Map.of(
                "records", records,
                "unreadCount", records.stream().filter(notice -> Boolean.FALSE.equals(notice.get("isRead"))).count(),
                "categorySummary", List.of(
                        Map.of("category", "教务通知", "label", "1 条未读"),
                        Map.of("category", "课程组通知", "label", "1 条未读"),
                        Map.of("category", "行政通知", "label", "1 条已读")
                )
        );
    }

    public Map<String, Object> mockSend(Map<String, Object> payload) {
        return Map.of(
                "taskNo", "NT-202604-0001",
                "status", "QUEUED",
                "payload", payload
        );
    }

    public void acceptWorkflowEvent(DomainEventMessage message) {
        workflowInbox.addFirst(Map.of(
                "eventType", message.eventType(),
                "routingKey", message.routingKey(),
                "sourceService", message.sourceService(),
                "bizType", message.bizType(),
                "bizId", message.bizId(),
                "title", message.title(),
                "detail", message.detail(),
                "occurredAt", message.occurredAt().toString(),
                "payload", message.payload()
        ));

        while (workflowInbox.size() > 20) {
            workflowInbox.removeLast();
        }
    }

    public List<Map<String, Object>> workflowInbox() {
        return new ArrayList<>(workflowInbox);
    }

    private List<Map<String, Object>> studentNoticeRecords() {
        return List.of(
                Map.of("id", 1, "title", "2025-2026 学年第二学期课程期中考核安排发布", "category", "教学通知", "publisher", "教务管理中心", "publishTime", "2026-04-17 09:20", "isRead", false, "summary", "课程期中考核将于下周三至周五进行，请按考场安排提前 15 分钟到场。", "priority", "high"),
                Map.of("id", 2, "title", "住宿费缴费提醒", "category", "财务通知", "publisher", "财务中心", "publishTime", "2026-04-16 16:40", "isRead", false, "summary", "本月住宿费将于 04-25 截止，请在缴费中心完成支付或提交缓缴申请。", "priority", "high"),
                Map.of("id", 3, "title", "创新实践周项目报名开始", "category", "校园活动", "publisher", "学生发展中心", "publishTime", "2026-04-15 14:00", "isRead", true, "summary", "创新实践周项目报名开放至本周五 17:00，可通过辅导员或第二课堂系统报名。", "priority", "normal"),
                Map.of("id", 4, "title", "图书馆研修室预约规则更新", "category", "校园公告", "publisher", "图书馆", "publishTime", "2026-04-14 10:30", "isRead", true, "summary", "新增晚间研修室预约时段与逾期未签到处理流程，请及时查看。", "priority", "normal")
        );
    }

    private List<Map<String, Object>> teacherNoticeRecords() {
        return List.of(
                Map.of("id", 101, "title", "期中考核监考安排已发布", "category", "教务通知", "publisher", "教务管理中心", "publishTime", "2026-04-17 08:20", "isRead", false, "summary", "请任课教师在今天 17:00 前确认监考场次，如有冲突请提交调换申请。", "priority", "high"),
                Map.of("id", 102, "title", "大学英语课程组周例会提醒", "category", "课程组通知", "publisher", "基础教学部", "publishTime", "2026-04-16 15:30", "isRead", false, "summary", "本周例会将讨论阶段考核命题质量与作业批改节奏，请提前准备反馈。", "priority", "normal"),
                Map.of("id", 103, "title", "教师培训签到方式调整", "category", "行政通知", "publisher", "信息服务中心", "publishTime", "2026-04-15 11:00", "isRead", true, "summary", "本周起培训签到改为工号加动态码双重校验，请提前检查账号绑定状态。", "priority", "normal")
        );
    }
}

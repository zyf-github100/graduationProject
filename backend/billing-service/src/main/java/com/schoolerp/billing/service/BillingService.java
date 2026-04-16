package com.schoolerp.billing.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BillingService {
    private final List<Map<String, Object>> bills = List.of(
            Map.of("id", 3001L, "billNo", "BL-202604-1001", "studentName", "林嘉禾", "className", "2025级软件工程1班", "feeItemName", "2026 春季学费", "receivableAmount", 9600, "receivedAmount", 9600, "dueDate", "2026-04-18", "status", "PAID"),
            Map.of("id", 3002L, "billNo", "BL-202604-1019", "studentName", "陈思齐", "className", "2025级软件工程3班", "feeItemName", "2026 春季学费", "receivableAmount", 9600, "receivedAmount", 4800, "dueDate", "2026-04-20", "status", "PARTIAL_PAID"),
            Map.of("id", 3003L, "billNo", "BL-202604-1032", "studentName", "赵嘉诚", "className", "2024级数据科学与大数据技术2班", "feeItemName", "实验耗材费", "receivableAmount", 420, "receivedAmount", 0, "dueDate", "2026-04-12", "status", "OVERDUE"),
            Map.of("id", 3004L, "billNo", "BL-202604-1036", "studentName", "何若彤", "className", "2024级网络工程1班", "feeItemName", "住宿费", "receivableAmount", 1800, "receivedAmount", 0, "dueDate", "2026-04-22", "status", "PENDING")
    );

    public List<Map<String, Object>> summary() {
        return List.of(
                Map.of("title", "应收总额", "value", "¥ 328,600", "trend", "本月新增 46,800", "caption", "学费、住宿费、教材费", "tone", "primary"),
                Map.of("title", "已收总额", "value", "¥ 291,240", "trend", "收缴率 88.6%", "caption", "含线上回调与线下登记", "tone", "success"),
                Map.of("title", "逾期账单", "value", "14", "trend", "较上周减少 3 笔", "caption", "需财务与辅导员协同跟进", "tone", "warning")
        );
    }

    public Map<String, Object> studentOverview() {
        List<Map<String, Object>> studentBills = List.of(
                Map.of("id", 5001L, "billNo", "STU-202604-001", "studentName", "林嘉禾", "className", "2025级软件工程1班", "feeItemName", "2026 春季学费", "receivableAmount", 9600, "receivedAmount", 9600, "dueDate", "2026-03-15", "status", "PAID"),
                Map.of("id", 5002L, "billNo", "STU-202604-002", "studentName", "林嘉禾", "className", "2025级软件工程1班", "feeItemName", "住宿费", "receivableAmount", 1800, "receivedAmount", 0, "dueDate", "2026-04-25", "status", "PENDING"),
                Map.of("id", 5003L, "billNo", "STU-202604-003", "studentName", "林嘉禾", "className", "2025级软件工程1班", "feeItemName", "教材费", "receivableAmount", 800, "receivedAmount", 800, "dueDate", "2026-03-18", "status", "PAID")
        );

        return Map.of(
                "summary", List.of(
                        metric("本学期应缴", "¥12,200", "含学费、住宿费与教材费", "已按广州软件学院财务计划生成", "primary"),
                        metric("已完成缴费", "¥10,400", "已完成 85.2%", "线上支付与线下登记已同步", "success"),
                        metric("当前待缴", "¥1,800", "住宿费 04-25 到期", "逾期后将影响宿舍门禁续期", "warning")
                ),
                "bills", studentBills,
                "pendingBill", studentBills.stream()
                        .filter(bill -> "PENDING".equals(bill.get("status")))
                        .findFirst()
                        .orElse(null)
        );
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        return bills.stream()
                .filter(bill -> keyword == null || keyword.isBlank()
                        || (bill.get("billNo") + " " + bill.get("studentName") + " " + bill.get("feeItemName")).contains(keyword))
                .filter(bill -> status == null || status.isBlank() || status.equals(bill.get("status")))
                .toList();
    }

    public Map<String, Object> detail(Long billId) {
        return bills.stream()
                .filter(bill -> billId.equals(bill.get("id")))
                .findFirst()
                .map(bill -> Map.<String, Object>ofEntries(
                        Map.entry("id", bill.get("id")),
                        Map.entry("billNo", bill.get("billNo")),
                        Map.entry("studentName", bill.get("studentName")),
                        Map.entry("className", bill.get("className")),
                        Map.entry("feeItemName", bill.get("feeItemName")),
                        Map.entry("receivableAmount", bill.get("receivableAmount")),
                        Map.entry("receivedAmount", bill.get("receivedAmount")),
                        Map.entry("dueDate", bill.get("dueDate")),
                        Map.entry("status", bill.get("status")),
                        Map.entry("outstandingAmount", ((Integer) bill.get("receivableAmount")) - ((Integer) bill.get("receivedAmount")))
                ))
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, 404, "账单不存在"));
    }

    private Map<String, Object> metric(String title, String value, String trend, String caption, String tone) {
        return Map.of(
                "title", title,
                "value", value,
                "trend", trend,
                "caption", caption,
                "tone", tone
        );
    }
}

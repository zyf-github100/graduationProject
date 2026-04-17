package com.schoolerp.billing.service;

import com.schoolerp.billing.dto.BillGenerateRequest;
import com.schoolerp.billing.dto.ReceiptCreateRequest;
import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BillingService {
    private static final String DEMO_STUDENT_NO = "202501001";
    private static final String DEMO_STUDENT_NAME = "林嘉禾";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Map<Long, Map<String, Object>> billStore = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> receiptStore = new ConcurrentHashMap<>();
    private final AtomicLong billIdSequence = new AtomicLong(3007L);
    private final AtomicLong receiptIdSequence = new AtomicLong(9001L);
    private final AtomicLong taskIdSequence = new AtomicLong(8001L);

    public BillingService() {
        seedBills();
    }

    public List<Map<String, Object>> summary() {
        List<Map<String, Object>> bills = allBills();
        int receivable = bills.stream().mapToInt(bill -> intValue(bill.get("receivableAmount"))).sum();
        int received = bills.stream().mapToInt(bill -> intValue(bill.get("receivedAmount"))).sum();
        long overdueCount = bills.stream().filter(bill -> "OVERDUE".equals(bill.get("status"))).count();
        String receiveRate = receivable == 0 ? "0.0%" : String.format(Locale.ROOT, "%.1f%%", received * 100.0 / receivable);

        return List.of(
                metric("应收总额", formatCurrency(receivable), "当前账单池自动汇总", "学费、住宿费、教材费统一统计", "primary"),
                metric("已收总额", formatCurrency(received), "收缴率 " + receiveRate, "线上支付与人工登记统一回写", "success"),
                metric("逾期账单", String.valueOf(overdueCount), "需财务与辅导员协同跟进", "逾期记录会在管理端持续提醒", "warning")
        );
    }

    public Map<String, Object> studentOverview() {
        List<Map<String, Object>> studentBills = billsByStudent(DEMO_STUDENT_NO);
        int receivable = studentBills.stream().mapToInt(bill -> intValue(bill.get("receivableAmount"))).sum();
        int received = studentBills.stream().mapToInt(bill -> intValue(bill.get("receivedAmount"))).sum();
        int outstanding = receivable - received;

        Map<String, Object> pendingBill = studentBills.stream()
                .filter(bill -> !"PAID".equals(bill.get("status")))
                .findFirst()
                .map(this::copyMap)
                .orElse(null);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("summary", List.of(
                metric("本学期应缴", formatCurrency(receivable), "覆盖当前学生所有账单", "学生端优先聚焦待处理账单", "primary"),
                metric("已完成缴费", formatCurrency(received), "已完成 " + percentage(received, receivable), "支付成功后立即同步账单状态", "success"),
                metric("当前待缴", formatCurrency(outstanding), pendingBill == null ? "暂无待缴账单" : pendingBill.get("feeItemName") + " 待处理", pendingBill == null ? "当前没有需要处理的账单" : "请在 " + pendingBill.get("dueDate") + " 前完成支付", "warning")
        ));
        overview.put("bills", studentBills.stream().map(this::copyMap).toList());
        overview.put("pendingBill", pendingBill);
        return overview;
    }

    public List<Map<String, Object>> list(String keyword, String status) {
        return allBills().stream()
                .filter(bill -> keyword == null || keyword.isBlank()
                        || (stringValue(bill.get("billNo")) + " " + stringValue(bill.get("studentNo")) + " " + stringValue(bill.get("studentName")) + " " + stringValue(bill.get("feeItemName"))).contains(keyword))
                .filter(bill -> status == null || status.isBlank() || status.equals(bill.get("status")))
                .sorted(Comparator
                        .comparing((Map<String, Object> bill) -> sortWeight(stringValue(bill.get("status"))))
                        .thenComparing(bill -> stringValue(bill.get("dueDate"))))
                .map(this::copyMap)
                .toList();
    }

    public Map<String, Object> detail(Long billId) {
        Map<String, Object> bill = requireBill(billId);
        List<Map<String, Object>> receipts = receiptStore.getOrDefault(billId, List.of());
        int receivableAmount = intValue(bill.get("receivableAmount"));
        int receivedAmount = intValue(bill.get("receivedAmount"));

        Map<String, Object> detail = new LinkedHashMap<>(copyMap(bill));
        detail.put("outstandingAmount", receivableAmount - receivedAmount);
        detail.put("studentInfo", Map.of(
                "studentName", bill.get("studentName"),
                "studentNo", bill.get("studentNo"),
                "className", bill.get("className")
        ));
        detail.put("feeItemInfo", Map.of(
                "feeItemName", bill.get("feeItemName"),
                "receivableAmount", receivableAmount,
                "dueDate", bill.get("dueDate")
        ));
        detail.put("billDetails", List.of(
                Map.of("itemName", bill.get("feeItemName"), "amount", receivableAmount, "remark", "系统按收费规则生成"),
                Map.of("itemName", "已收款", "amount", receivedAmount, "remark", "收款明细见下方回单")
        ));
        detail.put("receipts", receipts.stream().map(this::copyMap).toList());
        detail.put("statusTimeline", buildStatusTimeline(bill, receipts));
        return detail;
    }

    public Map<String, Object> generate(BillGenerateRequest request) {
        long billId = billIdSequence.getAndIncrement();
        String now = now();
        int receivableAmount = request.receivableAmount();
        String billNo = "BL-202604-" + String.format("%04d", billId % 10000);

        Map<String, Object> bill = new LinkedHashMap<>();
        bill.put("id", billId);
        bill.put("billNo", billNo);
        bill.put("studentNo", request.studentNo());
        bill.put("studentName", request.studentName());
        bill.put("className", request.className());
        bill.put("feeItemName", request.feeItemName());
        bill.put("receivableAmount", receivableAmount);
        bill.put("receivedAmount", 0);
        bill.put("dueDate", request.dueDate());
        bill.put("status", resolveBillStatus(request.dueDate(), 0, receivableAmount));
        bill.put("createdAt", now);
        bill.put("updatedAt", now);

        billStore.put(billId, bill);
        receiptStore.putIfAbsent(billId, new ArrayList<>());

        return Map.of(
                "taskId", taskIdSequence.getAndIncrement(),
                "taskStatus", "PROCESSING",
                "billId", billId,
                "billNo", billNo
        );
    }

    public Map<String, Object> createReceipt(ReceiptCreateRequest request) {
        Map<String, Object> currentBill = new LinkedHashMap<>(requireBill(request.billId()));
        int receivableAmount = intValue(currentBill.get("receivableAmount"));
        int receivedAmount = intValue(currentBill.get("receivedAmount"));
        int outstandingAmount = receivableAmount - receivedAmount;

        if (outstandingAmount <= 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, "当前账单已结清，无需重复登记收款");
        }

        if (request.receiptAmount() > outstandingAmount) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, 400, "收款金额不能大于待缴金额");
        }

        int newReceivedAmount = receivedAmount + request.receiptAmount();
        String paymentTime = request.paymentTime() == null || request.paymentTime().isBlank() ? now() : request.paymentTime();
        long receiptId = receiptIdSequence.getAndIncrement();
        String receiptNo = "RC-202604-" + String.format("%04d", receiptId % 10000);

        currentBill.put("receivedAmount", newReceivedAmount);
        currentBill.put("status", resolveBillStatus(stringValue(currentBill.get("dueDate")), newReceivedAmount, receivableAmount));
        currentBill.put("updatedAt", now());
        billStore.put(request.billId(), currentBill);

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("receiptId", receiptId);
        receipt.put("receiptNo", receiptNo);
        receipt.put("billId", request.billId());
        receipt.put("receiptAmount", request.receiptAmount());
        receipt.put("paymentChannel", request.paymentChannel());
        receipt.put("paymentTime", paymentTime);
        receipt.put("sourceType", request.sourceType());

        receiptStore.computeIfAbsent(request.billId(), ignored -> new ArrayList<>()).add(0, receipt);

        return Map.of(
                "receiptId", receiptId,
                "receiptNo", receiptNo,
                "billId", request.billId(),
                "billStatus", currentBill.get("status"),
                "receivedAmount", newReceivedAmount,
                "outstandingAmount", receivableAmount - newReceivedAmount
        );
    }

    private List<Map<String, Object>> buildStatusTimeline(Map<String, Object> bill, List<Map<String, Object>> receipts) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(Map.of(
                "time", stringValue(bill.get("createdAt")),
                "type", "primary",
                "content", "账单生成：" + bill.get("feeItemName")
        ));
        for (Map<String, Object> receipt : receipts) {
            timeline.add(Map.of(
                    "time", stringValue(receipt.get("paymentTime")),
                    "type", "success",
                    "content", "收款登记：" + formatCurrency(intValue(receipt.get("receiptAmount"))) + " / " + receipt.get("paymentChannel")
            ));
        }
        timeline.sort(Comparator.comparing(item -> stringValue(item.get("time"))));
        return timeline;
    }

    private List<Map<String, Object>> billsByStudent(String studentNo) {
        return allBills().stream()
                .filter(bill -> studentNo.equals(bill.get("studentNo")))
                .sorted(Comparator.comparing(bill -> stringValue(bill.get("dueDate"))))
                .toList();
    }

    private List<Map<String, Object>> allBills() {
        return billStore.values().stream().toList();
    }

    private Map<String, Object> requireBill(Long billId) {
        Map<String, Object> bill = billStore.get(billId);
        if (bill == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "账单不存在");
        }
        return bill;
    }

    private void seedBills() {
        seedBill(3001L, "BL-202604-1001", DEMO_STUDENT_NO, DEMO_STUDENT_NAME, "2025级软件工程1班", "2026 春季学费", 9600, 9600, "2026-04-18", "2026-04-01 09:10");
        seedBill(3002L, "BL-202604-1002", DEMO_STUDENT_NO, DEMO_STUDENT_NAME, "2025级软件工程1班", "住宿费", 1800, 0, "2026-04-25", "2026-04-02 10:00");
        seedBill(3003L, "BL-202604-1003", DEMO_STUDENT_NO, DEMO_STUDENT_NAME, "2025级软件工程1班", "教材费", 800, 800, "2026-03-18", "2026-03-01 08:30");
        seedBill(3004L, "BL-202604-1019", "202501014", "陈思齐", "2025级软件工程1班", "2026 春季学费", 9600, 4800, "2026-04-20", "2026-04-01 13:40");
        seedBill(3005L, "BL-202604-1032", "202401122", "赵嘉语", "2024级数据科学与大数据技术2班", "实验耗材费", 420, 0, "2026-04-12", "2026-04-03 16:20");
        seedBill(3006L, "BL-202604-1036", "202401087", "何若彤", "2024级网络工程1班", "住宿费", 1800, 0, "2026-04-22", "2026-04-04 09:00");

        receiptStore.put(3001L, new ArrayList<>(List.of(
                receiptRecord(8101L, "RC-202604-1001", 3001L, 9600, "ALIPAY", "2026-04-09 14:20", "ONLINE_PAYMENT")
        )));
        receiptStore.put(3003L, new ArrayList<>(List.of(
                receiptRecord(8102L, "RC-202603-1008", 3003L, 800, "WECHAT", "2026-03-12 10:18", "ONLINE_PAYMENT")
        )));
        receiptStore.put(3004L, new ArrayList<>(List.of(
                receiptRecord(8103L, "RC-202604-1024", 3004L, 4800, "BANK_TRANSFER", "2026-04-11 11:05", "FINANCE_DESK")
        )));
    }

    private void seedBill(long id,
                          String billNo,
                          String studentNo,
                          String studentName,
                          String className,
                          String feeItemName,
                          int receivableAmount,
                          int receivedAmount,
                          String dueDate,
                          String createdAt) {
        Map<String, Object> bill = new LinkedHashMap<>();
        bill.put("id", id);
        bill.put("billNo", billNo);
        bill.put("studentNo", studentNo);
        bill.put("studentName", studentName);
        bill.put("className", className);
        bill.put("feeItemName", feeItemName);
        bill.put("receivableAmount", receivableAmount);
        bill.put("receivedAmount", receivedAmount);
        bill.put("dueDate", dueDate);
        bill.put("status", resolveBillStatus(dueDate, receivedAmount, receivableAmount));
        bill.put("createdAt", createdAt);
        bill.put("updatedAt", createdAt);
        billStore.put(id, bill);
        receiptStore.putIfAbsent(id, new ArrayList<>());
    }

    private Map<String, Object> receiptRecord(long receiptId,
                                              String receiptNo,
                                              long billId,
                                              int receiptAmount,
                                              String paymentChannel,
                                              String paymentTime,
                                              String sourceType) {
        return Map.of(
                "receiptId", receiptId,
                "receiptNo", receiptNo,
                "billId", billId,
                "receiptAmount", receiptAmount,
                "paymentChannel", paymentChannel,
                "paymentTime", paymentTime,
                "sourceType", sourceType
        );
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

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private String formatCurrency(int value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.CHINA);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(value);
    }

    private String percentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return "0.0%";
        }
        return String.format(Locale.ROOT, "%.1f%%", numerator * 100.0 / denominator);
    }

    private String resolveBillStatus(String dueDate, int receivedAmount, int receivableAmount) {
        if (receivedAmount >= receivableAmount) {
            return "PAID";
        }
        if (receivedAmount > 0) {
            return "PARTIAL_PAID";
        }
        return LocalDate.parse(dueDate, DATE_FORMATTER).isBefore(LocalDate.now()) ? "OVERDUE" : "PENDING";
    }

    private int sortWeight(String status) {
        return switch (status) {
            case "OVERDUE" -> 0;
            case "PARTIAL_PAID" -> 1;
            case "PENDING" -> 2;
            case "PAID" -> 3;
            default -> 9;
        };
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private String now() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
}

package com.schoolerp.master.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import com.schoolerp.master.dto.StudentContact;
import com.schoolerp.master.dto.StudentDetail;
import com.schoolerp.master.dto.StudentLog;
import com.schoolerp.master.dto.StudentRecord;
import com.schoolerp.master.dto.StudentSaveRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StudentService {
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Map<Long, StudentDetail> studentStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1005L);

    public StudentService() {
        seedStudents().forEach(student -> studentStore.put(student.id(), student));
    }

    public Map<String, Integer> summary() {
        return Map.of(
                "currentStudents", 1284,
                "onLeaveStudents", 18,
                "incompleteGuardianProfiles", 26,
                "weeklyNewRecords", 34
        );
    }

    public Map<String, Object> options() {
        return Map.of(
                "campuses", List.of("主校区", "实训校区"),
                "grades", List.of("2025级", "2024级", "2023级"),
                "statuses", List.of(
                        Map.of("label", "在读", "value", "ACTIVE"),
                        Map.of("label", "请假中", "value", "LEAVE"),
                        Map.of("label", "休学", "value", "SUSPENDED")
                ),
                "genders", List.of("男", "女")
        );
    }

    public List<StudentRecord> list(String keyword, String grade, String status) {
        return studentStore.values().stream()
                .sorted(Comparator.comparing(StudentDetail::id))
                .filter(student -> keyword == null || keyword.isBlank() || matchKeyword(student, keyword))
                .filter(student -> grade == null || grade.isBlank() || grade.equals(student.gradeName()))
                .filter(student -> status == null || status.isBlank() || status.equals(student.status()))
                .map(this::toRecord)
                .toList();
    }

    public StudentDetail detail(Long studentId) {
        StudentDetail studentDetail = studentStore.get(studentId);
        if (studentDetail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "学生档案不存在");
        }
        return studentDetail;
    }

    public Map<String, Object> studentProfile(String studentNo) {
        StudentDetail detail = detailByStudentNo(studentNo);
        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("baseInfo", List.of(
                        labeledItem("姓名", detail.studentName()),
                        labeledItem("学号", detail.studentNo()),
                        labeledItem("年级班级", detail.className()),
                        labeledItem("校区", detail.campus()),
                        labeledItem("宿舍", detail.dormitory()),
                        labeledItem("联系电话", detail.guardianPhone())
                )),
                Map.entry("schoolInfo", List.of(
                        labeledItem("辅导员", detail.classTeacher()),
                        labeledItem("学籍状态", statusLabel(detail.status())),
                        labeledItem("入学日期", detail.admissionDate()),
                        labeledItem("培养方向", "软件工程 / 智能应用开发")
                )),
                Map.entry("contacts", detail.contacts().stream()
                        .map(contact -> labeledItem(contact.label(), contact.name() + "（" + contact.relation() + "）" + contact.phone()))
                        .toList()),
                Map.entry("security", List.of(
                        labeledItem("上次登录", "2026-04-17 07:48 / " + detail.campus()),
                        labeledItem("绑定手机", detail.guardianPhone()),
                        labeledItem("校园卡状态", "正常"),
                        labeledItem("账号安全等级", "高")
                )),
                Map.entry("preferences", Map.of(
                        "scores", true,
                        "billing", true,
                        "campus", true
                ))
        ));
    }

    public StudentDetail create(StudentSaveRequest request) {
        long studentId = idGenerator.incrementAndGet();
        StudentDetail studentDetail = buildDetail(studentId, request, List.of(log("新建学生档案", "基础数据服务")));
        studentStore.put(studentId, studentDetail);
        return studentDetail;
    }

    public StudentDetail update(Long studentId, StudentSaveRequest request) {
        StudentDetail current = detail(studentId);
        List<StudentLog> logs = new java.util.ArrayList<>();
        logs.add(log("更新学生档案", "基础数据服务"));
        logs.addAll(current.logs());
        StudentDetail updated = buildDetail(studentId, request, logs);
        studentStore.put(studentId, updated);
        return updated;
    }

    private StudentDetail buildDetail(Long studentId, StudentSaveRequest request, List<StudentLog> logs) {
        List<StudentContact> contacts = request.contacts() != null && !request.contacts().isEmpty()
                ? request.contacts()
                : List.of(new StudentContact(
                "紧急联系人",
                defaultValue(request.guardianName(), "待补充"),
                "联系人",
                defaultValue(request.guardianPhone(), "待补充")
        ));

        return new StudentDetail(
                studentId,
                request.studentNo(),
                request.studentName(),
                request.gender(),
                defaultValue(request.gradeName(), "2025级"),
                defaultValue(request.className(), ""),
                defaultValue(request.status(), "ACTIVE"),
                defaultValue(request.admissionDate(), LocalDateTime.now().toLocalDate().toString()),
                defaultValue(request.guardianName(), ""),
                defaultValue(request.guardianPhone(), ""),
                defaultValue(request.idCardMasked(), ""),
                defaultValue(request.campus(), "主校区"),
                defaultValue(request.classTeacher(), ""),
                defaultValue(request.dormitory(), "未分配"),
                defaultValue(request.address(), ""),
                defaultValue(request.remark(), ""),
                contacts,
                logs
        );
    }

    private StudentRecord toRecord(StudentDetail detail) {
        return new StudentRecord(
                detail.id(),
                detail.studentNo(),
                detail.studentName(),
                detail.gender(),
                detail.gradeName(),
                detail.className(),
                detail.status(),
                detail.admissionDate(),
                detail.guardianName(),
                detail.guardianPhone()
        );
    }

    private StudentDetail detailByStudentNo(String studentNo) {
        return studentStore.values().stream()
                .filter(student -> student.studentNo().equals(studentNo))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, 404, "学生档案不存在"));
    }

    private boolean matchKeyword(StudentDetail detail, String keyword) {
        String query = keyword.trim();
        return String.join(" ",
                        detail.studentNo(),
                        detail.studentName(),
                        detail.guardianName(),
                        detail.guardianPhone())
                .contains(query);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private StudentLog log(String content, String actor) {
        return new StudentLog(LocalDateTime.now().format(LOG_TIME_FORMATTER), content, actor);
    }

    private Map<String, String> labeledItem(String label, String value) {
        return Map.of("label", label, "value", value);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "ACTIVE" -> "在读";
            case "LEAVE" -> "请假中";
            case "SUSPENDED" -> "休学";
            default -> status;
        };
    }

    private List<StudentDetail> seedStudents() {
        Map<Long, StudentDetail> seed = new LinkedHashMap<>();
        seed.put(1001L, new StudentDetail(
                1001L, "202501001", "林嘉禾", "女", "2025级", "2025级软件工程1班", "ACTIVE", "2025-09-01",
                "林先生", "138****1021", "4401**********2218", "主校区", "陈老师", "6栋 302",
                "广州市天河区中山大道西 ** 号", "学习状态稳定，Java 程序设计与英语课程表现突出。",
                List.of(
                        new StudentContact("紧急联系人", "林先生", "父亲", "138****1021"),
                        new StudentContact("备用联系人", "林女士", "母亲", "139****1208")
                ),
                List.of(
                        new StudentLog("2026-04-10 09:30", "完成学籍信息年度复核", "学生工作办公室 王老师"),
                        new StudentLog("2026-03-18 14:20", "行政班从 2025级软件工程2班 调整至 2025级软件工程1班", "软件工程学院 李老师"),
                        new StudentLog("2025-09-01 08:45", "新生建档完成", "系统导入任务")
                )
        ));
        seed.put(1002L, new StudentDetail(
                1002L, "202501002", "周明远", "男", "2025级", "2025级软件工程1班", "ACTIVE", "2025-09-01",
                "周女士", "139****3008", "4401**********5142", "主校区", "陈老师", "3栋 205",
                "广州市海珠区新港中路 ** 号", "高等数学基础扎实，建议继续参与算法训练营。",
                List.of(new StudentContact("紧急联系人", "周女士", "母亲", "139****3008")),
                List.of(
                        new StudentLog("2026-04-08 16:10", "完成紧急联系人信息核验", "辅导员 陈老师"),
                        new StudentLog("2025-09-01 08:50", "新生建档完成", "系统导入任务")
                )
        ));
        seed.put(1003L, new StudentDetail(
                1003L, "202501057", "陈思齐", "女", "2025级", "2025级软件工程3班", "LEAVE", "2025-09-01",
                "陈先生", "137****8820", "4401**********3035", "主校区", "李老师", "5栋 412",
                "广州市白云区同和路 ** 号", "当前因流感请假，返校后需补录实验课签到记录。",
                List.of(new StudentContact("紧急联系人", "陈先生", "父亲", "137****8820")),
                List.of(
                        new StudentLog("2026-04-16 08:50", "提交学生请假审批申请", "陈思齐"),
                        new StudentLog("2026-03-12 11:20", "更新紧急联系人电话", "学生工作办公室 王老师")
                )
        ));
        seed.put(1004L, new StudentDetail(
                1004L, "202402031", "赵嘉诚", "男", "2024级", "2024级数据科学与大数据技术2班", "ACTIVE", "2024-09-01",
                "赵女士", "136****2010", "4401**********7205", "实训校区", "赵老师", "东区 2栋 108",
                "广州市黄埔区科学大道 ** 号", "近三次阶段考核成绩稳定，需关注实训项目与社团安排协调。",
                List.of(new StudentContact("紧急联系人", "赵女士", "母亲", "136****2010")),
                List.of(
                        new StudentLog("2026-04-02 15:00", "实验耗材费账单已生成", "收费系统"),
                        new StudentLog("2025-09-03 09:10", "完成转入实训校区班级分配", "数据科学与大数据技术教研室")
                )
        ));
        seed.put(1005L, new StudentDetail(
                1005L, "202401118", "何若彤", "女", "2024级", "2024级网络工程1班", "SUSPENDED", "2024-09-01",
                "何先生", "135****4509", "4401**********1468", "实训校区", "周老师", "未分配",
                "广州市番禺区大学城外环西路 ** 号", "因个人原因办理休学，预计下学期申请复学。",
                List.of(new StudentContact("紧急联系人", "何先生", "父亲", "135****4509")),
                List.of(
                        new StudentLog("2026-03-28 14:30", "办理休学状态变更", "学生工作办公室 王老师"),
                        new StudentLog("2025-09-01 09:20", "新生建档完成", "系统导入任务")
                )
        ));
        return seed.values().stream().toList();
    }
}

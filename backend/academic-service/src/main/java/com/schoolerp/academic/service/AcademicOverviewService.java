package com.schoolerp.academic.service;

import com.schoolerp.academic.client.MasterProfileClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AcademicOverviewService {
    private final MasterProfileClient masterProfileClient;

    public AcademicOverviewService(MasterProfileClient masterProfileClient) {
        this.masterProfileClient = masterProfileClient;
    }

    public Map<String, Object> overview() {
        return Map.of(
                "summary", List.of(
                        Map.of("label", "当前学期", "value", "2025-2026 学年第二学期"),
                        Map.of("label", "本周课表调整", "value", "6 次"),
                        Map.of("label", "待发布成绩任务", "value", "3 项"),
                        Map.of("label", "今日缺勤记录", "value", "9 人次")
                ),
                "gradeTasks", List.of(
                        Map.of("taskName", "2025级 Java 程序设计期中成绩录入", "className", "2025级软件工程专业", "teacherName", "陈老师", "deadline", "2026-04-20 18:00", "status", "DRAFT"),
                        Map.of("taskName", "2024级高等数学平时成绩复核", "className", "2024级软件工程1班 - 2024级数据科学与大数据技术2班", "teacherName", "张老师", "deadline", "2026-04-18 12:00", "status", "APPROVING"),
                        Map.of("taskName", "2023级软件工程导论阶段考核发布", "className", "2023级软件工程专业", "teacherName", "黄老师", "deadline", "2026-04-17 20:00", "status", "PUBLISHED")
                ),
                "rosterPreview", List.of(
                        Map.of("studentNo", "202501001", "studentName", "林嘉禾", "className", "2025级软件工程1班", "attendance", "NORMAL", "score", "89", "status", "ACTIVE"),
                        Map.of("studentNo", "202501002", "studentName", "周明远", "className", "2025级软件工程1班", "attendance", "LATE", "score", "92", "status", "ACTIVE"),
                        Map.of("studentNo", "202501057", "studentName", "陈思齐", "className", "2025级软件工程3班", "attendance", "LEAVE", "score", "86", "status", "LEAVE")
                ),
                "timetablePreview", List.of(
                        Map.of("period", "第1节", "monday", "2025级软件工程1班 / 高等数学 / 张老师", "tuesday", "2025级软件工程2班 / 大学英语 / 刘老师", "wednesday", "2025级软件工程3班 / Java 程序设计 / 李老师", "thursday", "2025级软件工程1班 / 数据结构 / 赵老师", "friday", "2025级软件工程2班 / 计算机网络 / 王老师"),
                        Map.of("period", "第2节", "monday", "2025级软件工程3班 / 大学英语 / 李老师", "tuesday", "2025级软件工程1班 / 高等数学 / 张老师", "wednesday", "2025级软件工程2班 / 离散数学 / 周老师", "thursday", "2025级软件工程3班 / 数据库原理 / 何老师", "friday", "2025级软件工程1班 / Java 程序设计 / 陈老师")
                )
        );
    }

    public Map<String, Object> studentHome() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metrics", List.of(
                metric("今日课程", "4 节", "下午还有 2 节课", "第 3 节 Java 程序设计 13:30 开始", "primary", "up", List.of(26, 28, 30, 31, 29, 33, 34, 36)),
                metric("本周签到", "100%", "连续 12 次课程签到正常", "今日上午签到与实验课签到已完成", "success", "up", List.of(82, 84, 87, 88, 91, 93, 95, 98)),
                metric("待缴费用", "¥1,800", "住宿费 04-25 截止", "当前有 1 笔账单待处理", "warning", "flat", List.of(58, 58, 56, 55, 55, 52, 50, 50)),
                metric("未读通知", "4 条", "2 条来自教务管理中心", "包含课程考核安排与校园通知", "danger", "up", List.of(12, 18, 14, 19, 16, 20, 24, 27))
        ));
        response.put("todayCourses", studentTodayCourses());
        response.put("upcomingItems", studentUpcomingItems());
        response.put("timeline", List.of(
                Map.of("time", "今天 08:00", "content", "完成课程签到", "actor", "学习事务系统"),
                Map.of("time", "今天 11:45", "content", "Java 实验报告已提交", "actor", "实验平台"),
                Map.of("time", "今天 15:30", "content", "创新实践周通知已确认", "actor", "第二课堂平台")
        ));
        response.put("latestNotices", studentNoticeRecords().subList(0, 3));
        response.put("profileSnapshot", loadStudentProfile());
        return response;
    }

    public Map<String, Object> studentSchedule() {
        return Map.of(
                "summary", List.of(
                        Map.of("label", "行政班", "value", "2025级软件工程1班"),
                        Map.of("label", "本周课程", "value", "26 节"),
                        Map.of("label", "本周重点", "value", "周五 Java 程序设计上机考核")
                ),
                "weeklySchedule", List.of(
                        Map.of("period", "第1节", "time", "08:00 - 08:45", "monday", "高等数学 / 张老师 / A301", "tuesday", "大学英语 / 李老师 / A305", "wednesday", "Java 程序设计 / 刘老师 / A204", "thursday", "数据结构 / 周老师 / 实验楼 201", "friday", "高等数学 / 张老师 / A301"),
                        Map.of("period", "第2节", "time", "09:00 - 09:45", "monday", "大学英语 / 李老师 / A305", "tuesday", "数据库原理 / 何老师 / 实验楼 203", "wednesday", "高等数学 / 张老师 / A301", "thursday", "离散数学 / 孙老师 / A206", "friday", "计算机网络 / 吴老师 / 实验楼 202"),
                        Map.of("period", "第3节", "time", "10:05 - 10:50", "monday", "Java 程序设计 / 刘老师 / A204", "tuesday", "高等数学 / 张老师 / A301", "wednesday", "大学英语 / 李老师 / A305", "thursday", "大学体育 / 田老师 / 操场", "friday", "软件工程导论 / 胡老师 / A204"),
                        Map.of("period", "第4节", "time", "11:00 - 11:45", "monday", "数据结构 / 周老师 / 实验楼 201", "tuesday", "思政课 / 黄老师 / A208", "wednesday", "职业发展指导 / 陈老师 / 一教 203", "thursday", "高等数学 / 张老师 / A301", "friday", "大学英语 / 李老师 / A305"),
                        Map.of("period", "第5节", "time", "13:30 - 14:15", "monday", "数据库原理 / 何老师 / 实验楼 203", "tuesday", "软件工程导论 / 胡老师 / A204", "wednesday", "数据结构 / 周老师 / 实验楼 201", "thursday", "大学英语 / 李老师 / A305", "friday", "信息安全基础 / 曾老师 / 机房 2"),
                        Map.of("period", "第6节", "time", "14:30 - 15:15", "monday", "计算机网络 / 吴老师 / 实验楼 202", "tuesday", "大学体育 / 田老师 / 操场", "wednesday", "高等数学 / 张老师 / A301", "thursday", "软件工程导论 / 胡老师 / A204", "friday", "程序设计实践 / 徐老师 / 实训楼 103")
                ),
                "todayCourses", studentTodayCourses(),
                "upcomingItems", studentUpcomingItems()
        );
    }

    public Map<String, Object> studentScores() {
        return Map.of(
                "metrics", List.of(
                        metric("平均分", "89.3", "较上次阶段考核 +2.6", "本学期已发布 8 门课程成绩", "success", "up", List.of(68, 72, 74, 79, 81, 84, 87, 89)),
                        metric("专业内排名", "5 / 82", "进入前 10%", "Java 程序设计与英语成绩带动整体提升", "primary", "up", List.of(18, 16, 15, 12, 11, 8, 6, 5)),
                        metric("待发布课程", "2 门", "数据库原理、计算机网络待录入", "预计本周五前完成发布", "warning", "flat", List.of(6, 5, 5, 4, 4, 3, 3, 2))
                ),
                "records", List.of(
                        Map.of("subject", "高等数学", "continuousScore", 90, "midtermScore", 88, "finalScore", 92, "totalScore", 90, "rank", "7 / 82", "status", "PUBLISHED", "teacherName", "张老师", "comment", "课堂练习与作业完成度稳定，建议继续保持推导书写质量。"),
                        Map.of("subject", "Java 程序设计", "continuousScore", 95, "midtermScore", 93, "finalScore", 96, "totalScore", 95, "rank", "3 / 82", "status", "PUBLISHED", "teacherName", "刘老师", "comment", "代码规范与问题拆解能力突出，实验报告质量持续稳定。"),
                        Map.of("subject", "大学英语", "continuousScore", 91, "midtermScore", 87, "finalScore", 93, "totalScore", 90, "rank", "6 / 82", "status", "PUBLISHED", "teacherName", "李老师", "comment", "阅读与口语表现良好，建议继续加强学术写作细节。"),
                        Map.of("subject", "数据结构", "continuousScore", 86, "midtermScore", 0, "finalScore", 0, "totalScore", 86, "rank", "--", "status", "DRAFT", "teacherName", "周老师", "comment", "实验报告已提交，等待阶段考核成绩发布。"),
                        Map.of("subject", "数据库原理", "continuousScore", 84, "midtermScore", 0, "finalScore", 0, "totalScore", 84, "rank", "--", "status", "DRAFT", "teacherName", "何老师", "comment", "课程表现稳定，等待本周统一发布阶段成绩。")
                ),
                "analysis", List.of(
                        Map.of("label", "优势课程", "value", "Java 程序设计 95 分，专业第 3 名"),
                        Map.of("label", "稳定区间", "value", "高等数学、大学英语保持 90 分以上"),
                        Map.of("label", "待发布课程", "value", "数据结构、数据库原理本周内发布")
                )
        );
    }

    public Map<String, Object> teacherHome() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metrics", List.of(
                metric("今日授课", "4 节", "下午还有 2 节大学英语课", "含 1 次辅导员指导与 3 节课堂教学", "primary", "up", List.of(18, 24, 28, 31, 33, 36, 39, 42)),
                metric("待登记考勤", "2 个班级", "需在 16:30 前完成", "2025级软件工程1班、2025级软件工程2班待补录", "warning", "flat", List.of(8, 8, 7, 7, 6, 5, 4, 2)),
                metric("待录成绩", "42 份", "大学英语单元测验待录入", "本周五前需完成发布前复核", "danger", "down", List.of(66, 64, 61, 58, 55, 52, 50, 42)),
                metric("未读通知", "6 条", "3 条来自教务管理中心", "包含考核安排与教师会议通知", "success", "up", List.of(10, 14, 13, 16, 18, 21, 23, 26))
        ));
        response.put("todayCourses", teacherTodayCourses());
        response.put("tasks", teacherTasks());
        response.put("latestNotices", teacherNoticeRecords().subList(0, 3));
        response.put("profileSnapshot", loadTeacherProfile());
        return response;
    }

    private Map<String, Object> loadStudentProfile() {
        try {
            return masterProfileClient.studentProfile("202501001");
        } catch (Exception ignored) {
            return Map.of(
                    "source", "academic-service-fallback",
                    "baseInfo", List.of(
                            Map.of("label", "姓名", "value", "林嘉禾"),
                            Map.of("label", "学号", "value", "202501001")
                    )
            );
        }
    }

    private Map<String, Object> loadTeacherProfile() {
        try {
            return masterProfileClient.teacherProfile("T2020018");
        } catch (Exception ignored) {
            return Map.of(
                    "source", "academic-service-fallback",
                    "baseInfo", List.of(
                            Map.of("label", "姓名", "value", "陈老师"),
                            Map.of("label", "工号", "value", "T2020018")
                    )
            );
        }
    }

    public Map<String, Object> teacherSchedule() {
        return Map.of(
                "summary", Map.of("weeklyLessons", "18 学时", "homeroomLessons", "2 项"),
                "weeklySchedule", teacherWeeklySchedule(),
                "todayCourses", teacherTodayCourses(),
                "tips", List.of(
                        "2025级软件工程1班大学英语单元测验成绩需在今天 18:00 前完成录入。",
                        "2024级软件工程3班下午最后一节课调整至实训楼 B402，请提前通知学生。",
                        "本周五课程组例会需提交阶段考核讲评材料。"
                )
        );
    }

    public Map<String, Object> teacherClasses() {
        return Map.of(
                "classes", teacherClassesList(),
                "roster", teacherRoster()
        );
    }

    public Map<String, Object> teacherAttendance() {
        return Map.of(
                "filters", Map.of("className", "2025级软件工程1班", "date", "2026-04-17", "period", "第1节"),
                "classOptions", List.of(
                        Map.of("label", "2025级软件工程1班", "value", "2025级软件工程1班"),
                        Map.of("label", "2025级软件工程2班", "value", "2025级软件工程2班"),
                        Map.of("label", "2024级软件工程3班", "value", "2024级软件工程3班")
                ),
                "periodOptions", List.of(
                        Map.of("label", "第1节 08:00 - 08:45", "value", "第1节"),
                        Map.of("label", "第2节 09:00 - 09:45", "value", "第2节"),
                        Map.of("label", "第5节 13:30 - 14:15", "value", "第5节")
                ),
                "records", teacherAttendanceRecords()
        );
    }

    public Map<String, Object> teacherGrades() {
        return Map.of(
                "summary", List.of(
                        Map.of("label", "待录任务", "value", "1 个班级任务"),
                        Map.of("label", "待复核", "value", "1 个班级任务"),
                        Map.of("label", "已发布", "value", "1 个班级任务")
                ),
                "tasks", teacherGradeTasks(),
                "records", teacherGradeRecords(),
                "tips", List.of(
                        "发布前请确认缺考、缓考学生已按学院规则标记。",
                        "如需更改已发布成绩，请先发起复核流程。",
                        "成绩提交后将同步到学生端成绩页和专业统计报表。"
                )
        );
    }

    private Map<String, Object> metric(String title,
                                       String value,
                                       String trend,
                                       String caption,
                                       String tone,
                                       String direction,
                                       List<Integer> series) {
        return Map.of(
                "title", title,
                "value", value,
                "trend", trend,
                "caption", caption,
                "tone", tone,
                "direction", direction,
                "series", series
        );
    }

    private List<Map<String, Object>> studentTodayCourses() {
        return List.of(
                Map.of("id", 1, "courseName", "高等数学", "teacherName", "张老师", "time", "08:00 - 08:45", "location", "教学楼 A301", "status", "NORMAL"),
                Map.of("id", 2, "courseName", "大学英语", "teacherName", "李老师", "time", "09:00 - 09:45", "location", "教学楼 A305", "status", "NORMAL"),
                Map.of("id", 3, "courseName", "Java 程序设计", "teacherName", "刘老师", "time", "13:30 - 14:15", "location", "实验楼 201", "status", "INFO"),
                Map.of("id", 4, "courseName", "职业发展指导", "teacherName", "辅导员 陈老师", "time", "15:30 - 16:10", "location", "一教 203", "status", "INFO")
        );
    }

    private List<Map<String, Object>> studentUpcomingItems() {
        return List.of(
                Map.of("title", "高等数学随堂测验", "meta", "04-18 10:05 · 教学楼 A301"),
                Map.of("title", "软件工程专题讲座", "meta", "04-19 15:30 · 学术报告厅"),
                Map.of("title", "住宿费截止", "meta", "04-25 23:59 · 缴费中心")
        );
    }

    private List<Map<String, Object>> studentNoticeRecords() {
        return List.of(
                Map.of("id", 1, "title", "2025-2026 学年第二学期课程期中考核安排发布", "category", "教学通知", "publisher", "教务管理中心", "publishTime", "2026-04-17 09:20", "isRead", false, "summary", "课程期中考核将于下周三至周五进行，请按考场安排提前 15 分钟到场。", "priority", "high"),
                Map.of("id", 2, "title", "住宿费缴费提醒", "category", "财务通知", "publisher", "财务中心", "publishTime", "2026-04-16 16:40", "isRead", false, "summary", "本月住宿费将于 04-25 截止，请在缴费中心完成支付或提交缓缴申请。", "priority", "high"),
                Map.of("id", 3, "title", "创新实践周项目报名开始", "category", "校园活动", "publisher", "学生发展中心", "publishTime", "2026-04-15 14:00", "isRead", true, "summary", "创新实践周项目报名开放至本周五 17:00，可通过辅导员或第二课堂系统报名。", "priority", "normal"),
                Map.of("id", 4, "title", "图书馆研修室预约规则更新", "category", "校园公告", "publisher", "图书馆", "publishTime", "2026-04-14 10:30", "isRead", true, "summary", "新增晚间研修室预约时段与逾期未签到处理流程，请及时查看。", "priority", "normal")
        );
    }

    private List<Map<String, Object>> teacherTodayCourses() {
        return List.of(
                Map.of("id", 1, "className", "2025级软件工程1班", "courseName", "大学英语", "time", "08:00 - 08:45", "location", "教学楼 A305", "status", "NORMAL"),
                Map.of("id", 2, "className", "2025级软件工程2班", "courseName", "大学英语", "time", "09:00 - 09:45", "location", "教学楼 A308", "status", "NORMAL"),
                Map.of("id", 3, "className", "2025级软件工程1班", "courseName", "职业发展指导", "time", "15:30 - 16:10", "location", "一教 203", "status", "INFO"),
                Map.of("id", 4, "className", "2024级软件工程3班", "courseName", "大学英语", "time", "16:20 - 17:05", "location", "实训楼 B402", "status", "INFO")
        );
    }

    private List<Map<String, Object>> teacherTasks() {
        return List.of(
                Map.of("title", "完成 2025级软件工程1班 大学英语单元测验成绩录入", "deadline", "今天 18:00", "status", "WARNING"),
                Map.of("title", "补录 2025级软件工程2班 下午课堂签到", "deadline", "今天 16:30", "status", "INFO"),
                Map.of("title", "提交下周课程组备课提纲", "deadline", "明天 12:00", "status", "DANGER")
        );
    }

    private List<Map<String, Object>> teacherNoticeRecords() {
        return List.of(
                Map.of("id", 101, "title", "期中考核监考安排已发布", "category", "教务通知", "publisher", "教务管理中心", "publishTime", "2026-04-17 08:20", "isRead", false, "summary", "请任课教师在今天 17:00 前确认监考场次，如有冲突请提交调换申请。", "priority", "high"),
                Map.of("id", 102, "title", "大学英语课程组周例会提醒", "category", "课程组通知", "publisher", "基础教学部", "publishTime", "2026-04-16 15:30", "isRead", false, "summary", "本周例会将讨论阶段考核命题质量与作业批改节奏，请提前准备反馈。", "priority", "normal"),
                Map.of("id", 103, "title", "教师培训签到方式调整", "category", "行政通知", "publisher", "信息服务中心", "publishTime", "2026-04-15 11:00", "isRead", true, "summary", "本周起培训签到改为工号加动态码双重校验，请提前检查账号绑定状态。", "priority", "normal")
        );
    }

    private List<Map<String, Object>> teacherWeeklySchedule() {
        return List.of(
                Map.of("period", "第1节", "time", "08:00 - 08:45", "monday", "2025级软件工程1班 / 大学英语 / A305", "tuesday", "2025级软件工程2班 / 大学英语 / A308", "wednesday", "2024级软件工程3班 / 大学英语 / B402", "thursday", "2025级软件工程1班 / 大学英语 / A305", "friday", "2024级软件工程3班 / 大学英语 / B401"),
                Map.of("period", "第2节", "time", "09:00 - 09:45", "monday", "2025级软件工程2班 / 大学英语 / A308", "tuesday", "2024级软件工程3班 / 大学英语 / B401", "wednesday", "课程组备课 / 办公室", "thursday", "2024级软件工程3班 / 大学英语 / B402", "friday", "2025级软件工程1班 / 大学英语 / A305"),
                Map.of("period", "第3节", "time", "10:05 - 10:50", "monday", "2024级软件工程3班 / 大学英语 / B402", "tuesday", "2025级软件工程1班 / 大学英语 / A305", "wednesday", "2025级软件工程2班 / 大学英语 / A308", "thursday", "辅导员例会 / 行政楼 204", "friday", "2024级软件工程3班 / 大学英语 / B307"),
                Map.of("period", "第4节", "time", "11:00 - 11:45", "monday", "备课 / 基础教学部办公室", "tuesday", "2024级软件工程3班 / 大学英语 / B307", "wednesday", "2025级软件工程1班 / 职业发展指导 / 一教 203", "thursday", "2025级软件工程2班 / 大学英语 / A308", "friday", "备课 / 基础教学部办公室"),
                Map.of("period", "第5节", "time", "13:30 - 14:15", "monday", "2024级软件工程3班 / 大学英语 / B401", "tuesday", "备课 / 基础教学部办公室", "wednesday", "2024级软件工程3班 / 大学英语 / B307", "thursday", "2025级软件工程1班 / 大学英语 / A305", "friday", "2025级软件工程2班 / 大学英语 / A308"),
                Map.of("period", "第6节", "time", "14:30 - 15:15", "monday", "课程组研讨 / 行政楼 302", "tuesday", "2024级软件工程3班 / 大学英语 / B402", "wednesday", "备课 / 基础教学部办公室", "thursday", "2024级软件工程3班 / 大学英语 / B401", "friday", "2025级软件工程1班 / 单元测验讲评 / A305")
        );
    }

    private List<Map<String, Object>> teacherClassesList() {
        return List.of(
                Map.of("className", "2025级软件工程1班", "role", "辅导员", "studentCount", 42, "attendanceRate", "97.9%", "averageScore", "89.3", "pendingItems", "2 项"),
                Map.of("className", "2025级软件工程2班", "role", "任课教师", "studentCount", 41, "attendanceRate", "96.2%", "averageScore", "86.8", "pendingItems", "1 项"),
                Map.of("className", "2024级软件工程3班", "role", "任课教师", "studentCount", 39, "attendanceRate", "98.1%", "averageScore", "88.5", "pendingItems", "3 项")
        );
    }

    private List<Map<String, Object>> teacherRoster() {
        return List.of(
                Map.of("studentName", "林嘉禾", "studentNo", "202501001", "className", "2025级软件工程1班", "attendance", "NORMAL", "latestHomework", "已提交", "score", "95"),
                Map.of("studentName", "周明远", "studentNo", "202501002", "className", "2025级软件工程1班", "attendance", "LATE", "latestHomework", "已提交", "score", "88"),
                Map.of("studentName", "陈思齐", "studentNo", "202501057", "className", "2025级软件工程3班", "attendance", "LEAVE", "latestHomework", "待提交", "score", "84")
        );
    }

    private List<Map<String, Object>> teacherAttendanceRecords() {
        return List.of(
                Map.of("studentName", "林嘉禾", "studentNo", "202501001", "className", "2025级软件工程1班", "attendance", "NORMAL", "checkInTime", "08:01", "remark", "正常到课"),
                Map.of("studentName", "周明远", "studentNo", "202501002", "className", "2025级软件工程1班", "attendance", "LATE", "checkInTime", "08:08", "remark", "迟到 8 分钟"),
                Map.of("studentName", "陈思齐", "studentNo", "202501057", "className", "2025级软件工程3班", "attendance", "LEAVE", "checkInTime", "--", "remark", "已提交病假申请")
        );
    }

    private List<Map<String, Object>> teacherGradeTasks() {
        return List.of(
                Map.of("taskName", "2025级软件工程1班大学英语单元测验成绩录入", "className", "2025级软件工程1班", "deadline", "2026-04-17 18:00", "status", "TODO"),
                Map.of("taskName", "2025级软件工程2班大学英语月度成绩复核", "className", "2025级软件工程2班", "deadline", "2026-04-18 12:00", "status", "APPROVING"),
                Map.of("taskName", "2024级软件工程3班大学英语阶段考核发布", "className", "2024级软件工程3班", "deadline", "2026-04-19 20:00", "status", "DRAFT")
        );
    }

    private List<Map<String, Object>> teacherGradeRecords() {
        return List.of(
                Map.of("studentName", "林嘉禾", "studentNo", "202501001", "className", "2025级软件工程1班", "score", 95, "rank", "3 / 42", "status", "PUBLISHED"),
                Map.of("studentName", "周明远", "studentNo", "202501002", "className", "2025级软件工程1班", "score", 88, "rank", "11 / 42", "status", "PUBLISHED"),
                Map.of("studentName", "陈思齐", "studentNo", "202501057", "className", "2025级软件工程3班", "score", 84, "rank", "18 / 39", "status", "DRAFT")
        );
    }
}

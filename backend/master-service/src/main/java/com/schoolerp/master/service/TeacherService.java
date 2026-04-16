package com.schoolerp.master.service;

import com.schoolerp.common.api.BusinessException;
import com.schoolerp.common.api.ResultCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TeacherService {
    public Map<String, Object> teacherProfile(String teacherNo) {
        String normalizedTeacherNo = normalizeTeacherNo(teacherNo);
        if (!"T2020018".equals(normalizedTeacherNo)) {
            throw new BusinessException(ResultCode.NOT_FOUND, 404, "教师档案不存在");
        }

        return Map.of(
                "baseInfo", List.of(
                        labeledItem("姓名", "陈老师"),
                        labeledItem("工号", "T2020018"),
                        labeledItem("部门", "基础教学部大学英语教研室"),
                        labeledItem("岗位", "大学英语教师 / 辅导员"),
                        labeledItem("办公地点", "基础教学部办公室 204"),
                        labeledItem("联系电话", "138****2036")
                ),
                "teachingInfo", List.of(
                        labeledItem("主授科目", "大学英语"),
                        labeledItem("授课班级", "2025级软件工程1班、2025级软件工程2班、2024级软件工程3班"),
                        labeledItem("辅导员身份", "2025级软件工程1班辅导员"),
                        labeledItem("本周课时", "18 学时")
                ),
                "security", List.of(
                        labeledItem("上次登录", "2026-04-17 07:18 / 主校区办公网"),
                        labeledItem("账号安全等级", "高"),
                        labeledItem("双重验证", "已开启"),
                        labeledItem("在线会话", "2 个终端")
                )
        );
    }

    private String normalizeTeacherNo(String teacherNo) {
        if (teacherNo == null || teacherNo.isBlank() || "teacher.chen".equalsIgnoreCase(teacherNo)) {
            return "T2020018";
        }
        return teacherNo;
    }

    private Map<String, String> labeledItem(String label, String value) {
        return Map.of("label", label, "value", value);
    }
}

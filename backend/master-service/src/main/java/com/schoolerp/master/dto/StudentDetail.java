package com.schoolerp.master.dto;

import java.util.List;

public record StudentDetail(
        Long id,
        String studentNo,
        String studentName,
        String gender,
        String gradeName,
        String className,
        String status,
        String admissionDate,
        String guardianName,
        String guardianPhone,
        String idCardMasked,
        String campus,
        String classTeacher,
        String dormitory,
        String address,
        String remark,
        List<StudentContact> contacts,
        List<StudentLog> logs
) {
}

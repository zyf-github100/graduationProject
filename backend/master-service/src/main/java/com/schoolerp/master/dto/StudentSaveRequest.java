package com.schoolerp.master.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record StudentSaveRequest(
        @NotBlank(message = "不能为空") String studentName,
        @NotBlank(message = "不能为空") String studentNo,
        @NotBlank(message = "不能为空") String gender,
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
        List<StudentContact> contacts
) {
}

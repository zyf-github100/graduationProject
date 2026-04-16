package com.schoolerp.master.dto;

public record StudentRecord(
        Long id,
        String studentNo,
        String studentName,
        String gender,
        String gradeName,
        String className,
        String status,
        String admissionDate,
        String guardianName,
        String guardianPhone
) {
}

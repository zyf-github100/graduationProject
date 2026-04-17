package com.schoolerp.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BillGenerateRequest(
        @NotBlank(message = "学生姓名不能为空") String studentName,
        @NotBlank(message = "学号不能为空") String studentNo,
        @NotBlank(message = "班级不能为空") String className,
        @NotBlank(message = "费用项目不能为空") String feeItemName,
        @Min(value = 1, message = "应收金额必须大于 0") int receivableAmount,
        @NotBlank(message = "到期日期不能为空") String dueDate
) {
}

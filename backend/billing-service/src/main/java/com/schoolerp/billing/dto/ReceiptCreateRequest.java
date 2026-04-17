package com.schoolerp.billing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReceiptCreateRequest(
        @NotNull(message = "账单 ID 不能为空") Long billId,
        @Min(value = 1, message = "收款金额必须大于 0") int receiptAmount,
        @NotBlank(message = "支付渠道不能为空") String paymentChannel,
        String paymentTime,
        @NotBlank(message = "来源类型不能为空") String sourceType
) {
}

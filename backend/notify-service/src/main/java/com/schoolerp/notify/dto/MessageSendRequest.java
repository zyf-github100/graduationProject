package com.schoolerp.notify.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record MessageSendRequest(
        @NotBlank(message = "Template code cannot be blank") String templateCode,
        @NotBlank(message = "Title cannot be blank") String title,
        @NotBlank(message = "Summary cannot be blank") String summary,
        @NotBlank(message = "Channel cannot be blank") String channel,
        String bizType,
        String bizId,
        @NotEmpty(message = "Recipients cannot be empty") @Valid List<Recipient> recipients
) {
    public record Recipient(
            @Positive(message = "Bill ID must be greater than 0") long billId,
            @NotBlank(message = "Bill number cannot be blank") String billNo,
            @NotBlank(message = "Student number cannot be blank") String studentNo,
            @NotBlank(message = "Student name cannot be blank") String studentName,
            @NotBlank(message = "Class name cannot be blank") String className,
            @NotBlank(message = "Fee item name cannot be blank") String feeItemName,
            @Positive(message = "Outstanding amount must be greater than 0") int outstandingAmount,
            @NotBlank(message = "Due date cannot be blank") String dueDate
    ) {
    }
}

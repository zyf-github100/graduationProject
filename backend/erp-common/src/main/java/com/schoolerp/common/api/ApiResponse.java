package com.schoolerp.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String requestId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message, String requestId) {
        return new ApiResponse<>(ResultCode.SUCCESS.name(), message, data, requestId, OffsetDateTime.now());
    }

    public static ApiResponse<Void> error(ResultCode code, String message, String requestId) {
        return new ApiResponse<>(code.name(), message, null, requestId, OffsetDateTime.now());
    }
}

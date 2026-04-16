package com.schoolerp.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @NotBlank(message = "不能为空") String refreshToken
) {
}

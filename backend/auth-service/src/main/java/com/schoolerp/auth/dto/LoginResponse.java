package com.schoolerp.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        String displayName,
        String userType
) {
}

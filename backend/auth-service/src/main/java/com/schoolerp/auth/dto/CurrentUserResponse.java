package com.schoolerp.auth.dto;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        Long orgUnitId,
        String orgUnit
) {
}

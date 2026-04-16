package com.schoolerp.common.security;

import java.util.List;

public record CurrentUser(
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        String orgUnit
) {
}

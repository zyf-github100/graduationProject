package com.schoolerp.auth.dto;

import java.util.List;

public record PermissionResponse(
        List<String> permissionCodes,
        List<String> dataScopes,
        List<String> exportPermissions
) {
}

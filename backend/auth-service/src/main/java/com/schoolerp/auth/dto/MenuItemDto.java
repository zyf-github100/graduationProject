package com.schoolerp.auth.dto;

import java.util.List;

public record MenuItemDto(
        Long menuId,
        String menuName,
        String path,
        String component,
        String icon,
        List<MenuItemDto> children
) {
}

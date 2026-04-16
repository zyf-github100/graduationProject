package com.schoolerp.common.api;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        long pageNo,
        long pageSize,
        long total,
        long totalPages
) {
    public static <T> PageResult<T> of(List<T> records, long pageNo, long pageSize, long total) {
        long totalPages = total == 0 ? 0 : (long) Math.ceil((double) total / pageSize);
        return new PageResult<>(records, pageNo, pageSize, total, totalPages);
    }
}

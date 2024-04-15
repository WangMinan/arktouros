package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @author : [wangminan]
 * @description : 日志查询dto
 */
public record LogQueryDto(
        @NotNull(message = "pageNum should not be empty")
        @Positive(message = "pageNum should be positive")
        int pageNum,
        @NotNull(message = "pageSize should not be empty")
        @Positive(message = "pageSize should be positive")
        int pageSize,
        String serviceName,
        String traceId,
        String keyword,
        String keywordNotIncluded,
        String severityText,
        Long startTimestamp,
        Long endTimestamp
) {
}

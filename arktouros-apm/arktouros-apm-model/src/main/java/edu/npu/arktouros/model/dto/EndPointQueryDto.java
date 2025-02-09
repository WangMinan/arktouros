package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @author : [wangminan]
 * @description : 端点搜索
 */
public record EndPointQueryDto(
        @NotBlank(message = "serviceName should not be empty")
        String serviceName,
        @NotNull(message = "pageNum should not be null")
        @Positive(message = "pageNum should be positive")
        int pageNum,
        @NotNull(message = "pageSize should not be null")
        @Positive(message = "pageSize should be positive")
        int pageSize,
        Long startTimestamp,
        Long endTimestamp
) {
}

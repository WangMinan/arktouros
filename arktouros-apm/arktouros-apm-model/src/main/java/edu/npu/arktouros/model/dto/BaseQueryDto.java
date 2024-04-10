package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * @author : [wangminan]
 * @description : 基础搜索Dto
 */
public record BaseQueryDto(

        @NotNull(message = "pageNum should not be empty")
        @Positive(message = "pageNum should be positive")
        int pageNum,

        @NotNull(message = "pageSize should not be empty")
        @Positive(message = "pageSize should be positive")
        int pageSize,

        String query
) {
}

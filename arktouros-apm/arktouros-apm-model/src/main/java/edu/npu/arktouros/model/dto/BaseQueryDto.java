package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @author : [wangminan]
 * @description : 基础搜索Dto
 */
public record BaseQueryDto(
        @NotNull
        Integer pageNum,
        @NotNull
        Integer pageSize,
        String query
) {
}

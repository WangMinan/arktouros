package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @author : [wangminan]
 * @description : 基础搜索Dto
 */
public record BaseQueryDto(

        @NotNull
        @Min(1)
        Integer pageNum,

        @NotNull
        @Min(1)
        Integer pageSize,

        String query
) {
}

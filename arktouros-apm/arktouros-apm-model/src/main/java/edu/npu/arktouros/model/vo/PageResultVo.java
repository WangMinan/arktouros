package edu.npu.arktouros.model.vo;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 分页搜索结果
 */
public record PageResultVo <T> (
        Long total,
        List<T> data
) {
}

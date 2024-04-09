package edu.npu.arktouros.mapper.otel.search;

import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.vo.R;

/**
 * @author : [wangminan]
 * @description : 抽象搜索Mapper
 */
public abstract class SearchMapper {
    public abstract R getServiceList(BaseQueryDto queryDto);
}

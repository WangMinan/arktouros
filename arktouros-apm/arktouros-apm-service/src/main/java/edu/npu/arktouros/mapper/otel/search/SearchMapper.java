package edu.npu.arktouros.mapper.otel.search;

import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 抽象搜索Mapper
 */
public abstract class SearchMapper {
    public abstract List<Service> getServiceList(BaseQueryDto queryDto);
}

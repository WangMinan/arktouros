package edu.npu.arktouros.mapper.otel.search.h2;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : H2搜索Mapper
 */
public class H2SearchMapper extends SearchMapper {
    @Override
    public List<Service> getServiceList(BaseQueryDto queryDto) {
        return List.of();
    }
}

package edu.npu.arktouros.mapper.otel.search;

import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 抽象搜索Mapper
 */
public abstract class SearchMapper {
    public abstract R getServiceList(BaseQueryDto queryDto);

    public abstract List<Service> getServiceListFromNamespace(String namespace);

    public abstract List<Span> getSpanListByServiceNames(List<String> serviceNames);

    public abstract Service getServiceByName(String serviceName);
}

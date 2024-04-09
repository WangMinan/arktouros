package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;
import jakarta.annotation.Resource;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : 搜索服务
 */
@org.springframework.stereotype.Service
public class SearchServiceImpl implements SearchService {

    @Resource
    private SearchMapper searchMapper;


    @Override
    public R getServiceList(BaseQueryDto queryDto) {
        return searchMapper.getServiceList(queryDto);
    }

    @Override
    public R getTopology(String namespace) {
        // 拿到namespace下的所有服务
        List<Service> serviceList =
                searchMapper.getServiceListFromNamespace(namespace);
        // 根据服务搜索span
        List<String> serviceNames = serviceList.stream()
                .map(Service::getName)
                .toList();
        List<Span> originalSpanList =
                searchMapper.getSpanListByServiceNames(serviceNames);
        // 组织成有序的拓扑
        // 注意 可能有多条链路
        List<Span> rootSpans = originalSpanList.stream().filter(Span::isRoot).toList();
        return R.ok();
    }
}

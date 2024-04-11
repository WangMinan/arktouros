package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.ServiceTopologyNode;
import edu.npu.arktouros.model.otel.topology.SpanTopologyNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 搜索服务
 */
@Slf4j
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
                // 过滤到空串
                .filter(name -> !name.isEmpty())
                .toList();
        List<Span> originalSpanList =
                searchMapper.getSpanListByServiceNames(serviceNames);
        // 组织成有序的拓扑
        // 注意 可能有多条链路
        List<Span> rootSpans = originalSpanList.stream().filter(Span::isRoot).toList();
        List<ServiceTopologyNode> topServiceTopologyNodes = new ArrayList<>();
        List<ServiceTopologyNode> knownServiceTopologyNodes = new ArrayList<>();
        // 以每一个rootSpan为起点，找到对应的service 然后做dfs构建topology
        for (Span rootSpan : rootSpans) {
            Service rootService =
                    serviceList.stream()
                            .filter(service -> service.getName().equals(rootSpan.getServiceName()))
                            .findFirst().orElse(null);
            if (rootSpan == null) {
                continue;
            }
            ServiceTopologyNode serviceTopologyNode =
                    ServiceTopologyNode.builder().service(rootService).build();
            knownServiceTopologyNodes.add(serviceTopologyNode);
            organizeServiceTopology(serviceTopologyNode, rootSpan, knownServiceTopologyNodes, serviceList,
                    originalSpanList.stream().filter(span -> !span.isRoot()).toList());
            topServiceTopologyNodes.add(serviceTopologyNode);
        }
        // 对topTopologyNodes进行去重
        topServiceTopologyNodes = topServiceTopologyNodes.stream().distinct().toList();
        R r = new R();
        r.put("result", topServiceTopologyNodes);
        return r;
    }

    @Override
    public R getLogList(LogQueryDto logQueryDto) {
        return searchMapper.getLogListByQuery(logQueryDto);
    }

    @Override
    public R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto) {
        return searchMapper.getEndPointListByServiceName(endPointQueryDto);
    }

    @Override
    public R getSpanTopologyByTraceId(String traceId) {
        List<Span> originalSpanList = searchMapper.getSpanListByTraceId(traceId);
        // 找到唯一的rootSpan
        Span rootSpan = originalSpanList.stream().filter(Span::isRoot).findFirst().orElse(null);
        if (rootSpan == null) {
            log.warn("Can't find root span for traceId: {}", traceId);
            return R.ok();
        }
        SpanTopologyNode topNode = SpanTopologyNode.builder().span(rootSpan).build();
        organizeSpanTopology(topNode,
                originalSpanList.stream().filter(span -> !span.isRoot()).toList());
        R r = new R();
        r.put("result", topNode);
        return r;
    }

    private void organizeSpanTopology(SpanTopologyNode beforeNode,
                                      List<Span> otherSpans) {
        otherSpans.stream()
                .filter(span -> span.getParentSpanId().equals(beforeNode.getSpan().getId()))
                .forEach(span -> {
                    SpanTopologyNode childNode = SpanTopologyNode.builder().span(span).build();
                    beforeNode.getChildNodes().add(childNode);
                    childNode.getParentNodes().add(beforeNode);
                    organizeSpanTopology(childNode, otherSpans);
                });
    }

    private void organizeServiceTopology(ServiceTopologyNode beforeNode, Span beforeSpan,
                                         List<ServiceTopologyNode> knownServiceTopologyNodes,
                                         List<Service> serviceList, List<Span> otherSpans) {
        otherSpans.stream()
                // 找出child service
                .filter(span -> span.getParentSpanId().equals(beforeSpan.getParentSpanId()))
                .forEach(span -> {
                    // 查找对应服务
                    Service service =
                            serviceList.stream()
                                    .filter(s -> s.getName().equals(span.getServiceName()))
                                    .findFirst().orElse(null);
                    if (service == null) {
                        return;
                    }
                    // 查找service对应node是否已经存在 如果和beforeNode的Service一致则无需添加
                    ServiceTopologyNode childNode =
                            knownServiceTopologyNodes.stream()
                                    .filter(node ->
                                            node.getService().getName().equals(service.getName()))
                                    .findFirst().orElse(null);
                    if (childNode == null) {
                        childNode = ServiceTopologyNode.builder().service(service).build();
                        knownServiceTopologyNodes.add(childNode);
                        childNode.getParentNodes().add(beforeNode);
                        beforeNode.getChildNodes().add(childNode);
                    } else if (!childNode.equals(beforeNode)) {
                        childNode.getParentNodes().add(beforeNode);
                        beforeNode.getChildNodes().add(childNode);
                    }
                    // 继续递归搜索
                    organizeServiceTopology(childNode, span,
                            knownServiceTopologyNodes, serviceList, otherSpans);
                });
    }

}

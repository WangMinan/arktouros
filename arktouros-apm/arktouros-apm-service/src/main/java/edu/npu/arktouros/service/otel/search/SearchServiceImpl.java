package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.TopologyNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;
import jakarta.annotation.Resource;

import java.util.ArrayList;
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
                // 过滤到空串
                .filter(name -> !name.isEmpty())
                .toList();
        List<Span> originalSpanList =
                searchMapper.getSpanListByServiceNames(serviceNames);
        // 组织成有序的拓扑
        // 注意 可能有多条链路
        List<Span> rootSpans = originalSpanList.stream().filter(Span::isRoot).toList();
        List<TopologyNode> topTopologyNodes = new ArrayList<>();
        List<TopologyNode> knownTopologyNodes = new ArrayList<>();
        // 以每一个rootSpan为起点，找到对应的service 然后做dfs构建topology
        for (Span rootSpan : rootSpans) {
            Service rootService =
                    serviceList.stream()
                            .filter(service -> service.getName().equals(rootSpan.getServiceName()))
                            .findFirst().orElse(null);
            if (rootSpan == null) {
                continue;
            }
            TopologyNode topologyNode =
                    TopologyNode.builder().service(rootService).build();
            knownTopologyNodes.add(topologyNode);
            organizeTopology(topologyNode, rootSpan, knownTopologyNodes, serviceList,
                    originalSpanList.stream().filter(span -> !span.isRoot()).toList());
            topTopologyNodes.add(topologyNode);
        }
        // 对topTopologyNodes进行去重
        topTopologyNodes = topTopologyNodes.stream().distinct().toList();
        R r = new R();
        r.put("result", topTopologyNodes);
        return r;
    }

    private void organizeTopology(TopologyNode beforeNode, Span beforeSpan,
                                  List<TopologyNode> knownTopologyNodes,
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
                    TopologyNode childNode =
                            knownTopologyNodes.stream()
                                    .filter(node ->
                                            node.getService().getName().equals(service.getName()))
                                    .findFirst().orElse(null);
                    if (childNode == null) {
                        childNode = TopologyNode.builder().service(service).build();
                        knownTopologyNodes.add(childNode);
                        childNode.getParentNodes().add(beforeNode);
                        beforeNode.getChildNodes().add(childNode);
                    } else if (!childNode.equals(beforeNode)) {
                        childNode.getParentNodes().add(beforeNode);
                        beforeNode.getChildNodes().add(childNode);
                    }
                    // 继续递归搜索
                    organizeTopology(childNode, span, knownTopologyNodes, serviceList, otherSpans);
                });
    }

}

package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.service.Topology;
import edu.npu.arktouros.model.otel.topology.service.TopologyCall;
import edu.npu.arktouros.model.otel.topology.service.TopologyNode;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.MetricVo;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.model.vo.SpanTreeNodeVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author : [wangminan]
 * @description : 搜索服务
 */
@Slf4j
@org.springframework.stereotype.Service
public class SearchServiceImpl implements SearchService {

    @Resource
    private SearchMapper searchMapper;

    private static final String RESULT = "result";


    @Override
    public R getServiceList(ServiceQueryDto queryDto) {
        return searchMapper.getServiceList(queryDto);
    }

    @Override
    public R getServiceTopology(String namespace) {
        Topology<Service> topology = new Topology<>();
        // 拿到namespace下的所有服务
        List<Service> serviceList =
                searchMapper.getServiceListFromNamespace(namespace);
        // 根据服务搜索span
        List<String> serviceNames = serviceList.stream()
                .map(Service::getName)
                // 过滤空串
                .filter(name -> name != null && !name.isEmpty())
                .toList();
        List<Span> originalSpanList =
                searchMapper.getSpanListByServiceNames(serviceNames);
        // 组织成有序的拓扑
        // 注意 可能有多条链路
        List<Span> rootSpans = originalSpanList.stream()
                .filter(span -> span.isRoot()
                        || span.getParentSpanId() == null
                        || span.getParentSpanId().isEmpty())
                .toList();
        Set<TopologyNode<Service>> topologyNodes = new HashSet<>();
        Set<TopologyCall<Service>> topologyCalls = new HashSet<>();
        rootSpans.forEach(rootSpan -> {
            Service rootService = searchMapper.getServiceByName(rootSpan.getServiceName());
            TopologyNode<Service> rootNode = new TopologyNode<>(rootService);
            topologyNodes.add(rootNode);
            // 递归查找
            handleOtherSpansForServiceTopology(rootSpan,
                    rootNode,
                    // 过滤掉rootSpans中的
                    originalSpanList.stream().filter(span -> !rootSpans.contains(span)).toList(),
                    topologyNodes, topologyCalls);
        });
        topology.setNodes(topologyNodes);
        topology.setCalls(topologyCalls);
        R r = new R();
        r.put(RESULT, topology);
        return r;
    }

    private void handleOtherSpansForServiceTopology(
            Span formerSpan, TopologyNode<Service> formerNode,
            List<Span> otherSpans,
            Set<TopologyNode<Service>> topologyNodes,
            Set<TopologyCall<Service>> topologyCalls) {
        // 这个位置不应该用深度优先搜索 要用广度优先搜索
        for (Span otherSpan : otherSpans) {
            if (otherSpan.getParentSpanId().equals(formerSpan.getId())) {
                Service otherService = searchMapper.getServiceByName(otherSpan.getServiceName());
                TopologyNode<Service> otherNode = new TopologyNode<>(otherService);
                if (!formerNode.equals(otherNode)) {
                    topologyNodes.add(otherNode);
                    TopologyCall<Service> topologyCall =
                            new TopologyCall<>(formerNode, otherNode);
                    topologyCalls.add(topologyCall);
                }
                handleOtherSpansForServiceTopology(otherSpan, otherNode,
                        otherSpans.stream().filter(span -> !span.equals(otherSpan)).toList(),
                        topologyNodes, topologyCalls);
            }
        }
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
    public R getSpanTopologyByTraceId(SpanTopologyQueryDto spanTopologyQueryDto) {
        List<Span> originalSpanList =
                searchMapper.getSpanListByTraceId(spanTopologyQueryDto);
        // 找到唯一的rootSpan
        Span rootSpan = getRootSpan(originalSpanList);
        if (rootSpan == null) {
            log.warn("Can't find root span for spanTopologyQueryDto: {}", spanTopologyQueryDto);
            return R.ok();
        }
        SpanTreeNode.SpanTreeNodeBuilder rootBuilder = SpanTreeNode.builder();
        rootBuilder.span(rootSpan);
        SpanTreeNode rootTreeNode = rootBuilder.build();
        // 广度优先搜索
        buildTraceTree(List.of(rootTreeNode),
                originalSpanList.stream().filter(span -> !span.equals(rootSpan)).toList());
        R r = new R();
        r.put(RESULT, new SpanTreeNodeVo(rootTreeNode));
        return r;
    }

    private Span getRootSpan(List<Span> originalSpanList) {
        Span span = originalSpanList.stream()
                .filter(Span::isRoot).findFirst().orElse(null);
        if (span != null) {
            return span;
        }
        log.info("Root span is not in this service, try to find the first span without considering parent span id.");
        // 取startTimeUnixNano最小的
        return originalSpanList.stream()
                .min((span1, span2) -> (int)
                        (span1.getStartTime() - span2.getStartTime()))
                .orElse(null);
    }

    private void buildTraceTree(List<SpanTreeNode> formerLayerSpans, List<Span> otherSpans) {
        List<Span> currentLayerSpans = new ArrayList<>();
        List<SpanTreeNode> currentLayerNodes = new ArrayList<>();
        formerLayerSpans.forEach(formerSpan -> otherSpans.forEach(otherSpan -> {
            if (otherSpan.getParentSpanId().equals(formerSpan.getSpan().getId())) {
                SpanTreeNode.SpanTreeNodeBuilder otherBuilder =
                        SpanTreeNode.builder().span(otherSpan);
                SpanTreeNode spanTreeNode = otherBuilder.build();
                currentLayerSpans.add(otherSpan);
                currentLayerNodes.add(spanTreeNode);
                formerSpan.getChildren().add(spanTreeNode);
            }
        }));
        if (!currentLayerSpans.isEmpty()) {
            buildTraceTree(currentLayerNodes, otherSpans.stream()
                    .filter(span -> !currentLayerSpans.contains(span)).toList());
        }
    }

    @Override
    public R getMetrics(MetricQueryDto metricQueryDto) {
        // 先取name，然后用name来搜 做两次搜索
        List<String> metricNames = searchMapper.getMetricsNames(metricQueryDto.serviceName(),
                metricQueryDto.metricNameLimit());
        if (metricNames.isEmpty()) {
            return R.ok();
        }
        List<Metric> metricValues = searchMapper.getMetricsValues(metricNames,
                metricQueryDto.serviceName(),
                metricQueryDto.startTimeStamp(), metricQueryDto.endTimeStamp());
        // 整形 最后要送出去一个MetricVo类型的List 需要做三层聚合 只有name serviceName和metricType字段一样的才能放到一个桶里
        List<MetricVo> metricVoList = new ArrayList<>();
        metricValues.forEach(metric -> {
            boolean isFound = false;
            // 桶里新加
            for (MetricVo metricVo : metricVoList) {
                if (metricVo.name().equals(metric.getName()) &&
                        metricVo.serviceName().equals(metric.getServiceName()) &&
                        metricVo.metricType().equals(metric.getMetricType())) {
                    metricVo.metrics().add(metric);
                    isFound = true;
                    break;
                }
            }
            // 开新的一个桶
            if (!isFound) {
                metricVoList.add(new MetricVo(metric.getName(), metric.getServiceName(),
                        metric.getMetricType(), new ArrayList<>(List.of(metric))));
            }
        });
        R r = new R();
        r.put(RESULT, metricVoList);
        return r;
    }

    @Override
    public R getNamespaceList(String query) {
        return searchMapper.getNamespaceList(query);
    }

    @Override
    public R getAllLogLevels(String query) {
        return searchMapper.getAllLogLevels(query);
    }
}

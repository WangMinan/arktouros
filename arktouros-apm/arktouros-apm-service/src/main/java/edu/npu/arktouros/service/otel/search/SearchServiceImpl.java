package edu.npu.arktouros.service.otel.search;

import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.Topology;
import edu.npu.arktouros.model.otel.topology.TopologyCall;
import edu.npu.arktouros.model.otel.topology.TopologyNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.MetricVo;
import edu.npu.arktouros.model.vo.R;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
                // 过滤到空串
                .filter(name -> name != null && !name.isEmpty())
                .toList();
        List<Span> originalSpanList =
                searchMapper.getSpanListByServiceNames(serviceNames);
        // 组织成有序的拓扑
        // 注意 可能有多条链路
        List<Span> rootSpans = originalSpanList.stream().filter(Span::isRoot).toList();
        List<TopologyNode<Service>> topologyNodes = new ArrayList<>();
        List<TopologyCall<Service>> topologyCalls = new ArrayList<>();
        rootSpans.forEach(rootSpan -> {
            Service rootService = searchMapper.getServiceByName(rootSpan.getServiceName());
            TopologyNode<Service> rootNode = new TopologyNode<>(rootService);
            topologyNodes.add(rootNode);
            // 递归查找
            handleOtherSpansForServiceTopology(rootSpan,
                    rootNode,
                    // 过滤掉rootSpan
                    rootSpans.stream().filter(span -> !span.equals(rootSpan)).toList(),
                    topologyNodes, topologyCalls);
        });
        topology.setNodes(topologyNodes);
        topology.setCalls(topologyCalls);
        R r = new R();
        r.put("result", topology);
        return r;
    }

    private void handleOtherSpansForServiceTopology(
            Span formerSpan, TopologyNode<Service> formerNode,
            List<Span> otherSpans,
            List<TopologyNode<Service>> topologyNodes,
            List<TopologyCall<Service>> topologyCalls) {
        otherSpans.forEach(otherSpan -> {
            if (otherSpan.getParentSpanId().equals(formerSpan.getId())) {
                Service otherService = searchMapper.getServiceByName(otherSpan.getServiceName());
                TopologyNode<Service> otherNode = new TopologyNode<>(otherService);
                topologyNodes.add(otherNode);
                TopologyCall<Service> topologyCall = new TopologyCall<>(formerNode, otherNode);
                topologyCalls.add(topologyCall);
                handleOtherSpansForServiceTopology(otherSpan, otherNode,
                        otherSpans.stream().filter(span -> !span.equals(otherSpan)).toList(),
                        topologyNodes, topologyCalls);
            }
        });
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
        Topology<Span> topology = new Topology<>();
        List<TopologyNode<Span>> topologyNodes = new ArrayList<>();
        List<TopologyCall<Span>> topologyCalls = new ArrayList<>();
        // 找到唯一的rootSpan
        Span rootSpan = originalSpanList.stream()
                .filter(Span::isRoot).findFirst().orElse(null);
        if (rootSpan == null) {
            log.warn("Can't find root span for traceId: {}", traceId);
            return R.ok();
        }
        TopologyNode<Span> rootNode = new TopologyNode<>(rootSpan);
        topologyNodes.add(rootNode);
        // 递归查找
        handleOtherSpansForTraceTopology(rootSpan,
                rootNode,
                // 过滤掉rootSpan
                originalSpanList.stream().filter(span -> !span.equals(rootSpan)).toList(),
                topologyNodes, topologyCalls);
        topology.setNodes(topologyNodes);
        topology.setCalls(topologyCalls);
        R r = new R();
        r.put("result", topology);
        return r;
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
        r.put("result", metricVoList);
        return r;
    }

    @Override
    public R getNamespaceList(String query) {
        return searchMapper.getNamespaceList(query);
    }

    private void handleOtherSpansForTraceTopology(
            Span formerSpan, TopologyNode<Span> formerNode,
            List<Span> otherSpans,
            List<TopologyNode<Span>> topologyNodes,
            List<TopologyCall<Span>> topologyCalls) {
        otherSpans.forEach(otherSpan -> {
            if (otherSpan.getParentSpanId().equals(formerSpan.getId())) {
                TopologyNode<Span> otherNode = new TopologyNode<>(otherSpan);
                topologyNodes.add(otherNode);
                TopologyCall<Span> topologyCall = new TopologyCall<>(formerNode, otherNode);
                topologyCalls.add(topologyCall);
                handleOtherSpansForTraceTopology(otherSpan, otherNode,
                        otherSpans.stream().filter(span -> !span.equals(otherSpan)).toList(),
                        topologyNodes, topologyCalls);
            }
        });
    }

}

package edu.npu.arktouros.service.search;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.MetricQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanNamesQueryDto;
import edu.npu.arktouros.model.dto.SpanTimesQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.service.Topology;
import edu.npu.arktouros.model.otel.topology.service.TopologyCall;
import edu.npu.arktouros.model.otel.topology.service.TopologyNode;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.model.vo.MetricVo;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.model.vo.SpanTimesVo;
import edu.npu.arktouros.model.vo.SpanTreeNodeVo;
import edu.npu.arktouros.util.StandardDeviationUtil;
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
    public R getServiceTopology(String namespace, Long timestamp) {
        Topology<Service> topology = new Topology<>();
        // 拿到namespace下的所有服务
        List<Service> serviceList =
                searchMapper.getServiceListFromTopologyQuery(namespace);
        // 根据服务搜索span
        List<String> serviceNames = serviceList.stream()
                .map(Service::getName)
                // 过滤空串
                .filter(name -> name != null && !name.isEmpty())
                .toList();
        // 截取所有小于等于timestamp的span 也就是说span的产生时间需要早于给定时间戳
        // 对每个serviceName只保留最新的一个span
        List<Span> originalSpanList = searchMapper.getSpanListByServiceNames(serviceNames)
                .stream()
                .filter(span -> span.getEndTime() <= timestamp)
                // 按serviceName分组，并保留每个组中endTime最大的span
                .collect(java.util.stream.Collectors.toMap(
                        Span::getName,
                        span -> span,
                        (existingSpan, newSpan) -> existingSpan.getEndTime() > newSpan.getEndTime() ? existingSpan : newSpan
                ))
                .values()
                .stream()
                .toList();

        // 改为批量处理所有span，而不是逐个处理
        StandardDeviationUtil.markLongDurationSpansBatch(searchMapper, originalSpanList);

        // 后续代码不变
        List<Span> rootSpans = originalSpanList.stream()
                .filter(span -> span.isRoot()
                        || span.getParentSpanId() == null
                        || span.getParentSpanId().isEmpty())
                .toList();
        Set<TopologyNode<Service>> topologyNodes = new HashSet<>();
        Set<TopologyCall<Service>> topologyCalls = new HashSet<>();
        rootSpans.forEach(rootSpan -> {
            Service rootService = searchMapper.getServiceByName(rootSpan.getServiceName());
            tagServiceAndRewriteStatus(rootSpan, rootService);
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
                    // 对超时service打标
                    tagServiceAndRewriteStatus(otherSpan, otherService);
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

    private void tagServiceAndRewriteStatus(Span span, Service service) {
        if (span.getTags().stream().anyMatch(tag -> tag.getKey().equals(Span.SpanTagKey.LONG_DURATION.getKey()))) {
            if (service.getTags() == null) {
                service.setTags(new ArrayList<>());
            }
            service.getTags().add(new Tag(
                    Span.SpanTagKey.LONG_DURATION.getKey(),
                    span.getName()
            ));
            service.setStatus(false);
        } else {
            service.setStatus(true);
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
        SpanTreeNodeVo spanTreeNodeVo = new SpanTreeNodeVo(
                generateSpanTree(rootSpan, originalSpanList));
        R r = new R();
        r.put(RESULT, spanTreeNodeVo);
        return r;
    }

    /**
     * 生成span树
     *
     * @param rootSpan         根节点Span
     * @param originalSpanList 在同一TraceId下的Span列表
     * @return Span树节点
     */
    private SpanTreeNode generateSpanTree(Span rootSpan, List<Span> originalSpanList) {
        StandardDeviationUtil.markLongDurationSpans(searchMapper, rootSpan);
        SpanTreeNode.SpanTreeNodeBuilder rootBuilder = SpanTreeNode.builder();
        rootBuilder.span(rootSpan);
        SpanTreeNode rootTreeNode = rootBuilder.build();
        // 广度优先搜索
        dfsForBuilding(List.of(rootTreeNode),
                originalSpanList.stream().filter(span -> !span.equals(rootSpan)).toList());
        return rootTreeNode;
    }

    private Span getRootSpan(List<Span> originalSpanList) {
        Span span = originalSpanList.stream()
                .filter(spanItem -> spanItem.isRoot()
                        || spanItem.getParentSpanId() == null
                        || spanItem.getParentSpanId().isEmpty())
                .findFirst().orElse(null);
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

    private void dfsForBuilding(List<SpanTreeNode> formerLayerSpans, List<Span> otherSpans) {
        List<Span> currentLayerSpans = new ArrayList<>();
        List<SpanTreeNode> currentLayerNodes = new ArrayList<>();
        formerLayerSpans.forEach(formerSpan -> otherSpans.forEach(otherSpan -> {
            if (otherSpan.getParentSpanId().equals(formerSpan.getSpan().getId())) {
                // 均方差打标
                StandardDeviationUtil.markLongDurationSpans(searchMapper, otherSpan);
                SpanTreeNode.SpanTreeNodeBuilder otherBuilder =
                        SpanTreeNode.builder().span(otherSpan);
                SpanTreeNode spanTreeNode = otherBuilder.build();
                currentLayerSpans.add(otherSpan);
                currentLayerNodes.add(spanTreeNode);
                if (!(formerSpan.getChildren() instanceof ArrayList<SpanTreeNode>)) {
                    formerSpan.setChildren(new ArrayList<>(formerSpan.getChildren()));
                }
                formerSpan.getChildren().add(spanTreeNode);
            }
        }));
        if (!currentLayerSpans.isEmpty()) {
            dfsForBuilding(currentLayerNodes, otherSpans.stream()
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

    @Override
    public List<Service> getAllServices() {
        return searchMapper.getAllServices();
    }

    @Override
    public int getTraceCount(Service service, long startTime, long endTime) {
        return searchMapper.getTraceCount(service, startTime, endTime);
    }

    @Override
    public List<SpanTreeNode> getSpanTreeInMinutes(String serviceName, String traceId,
                                                   long startTime, long endTime) {
        List<Span> originalSpanList =
                searchMapper.getSpanListByTraceId(serviceName, traceId, startTime, endTime);
        Span rootSpan = getRootSpan(originalSpanList);
        if (rootSpan == null) {
            log.warn("Can't find root span for traceId: {}", traceId);
            return List.of();
        }
        return List.of(generateSpanTree(rootSpan, originalSpanList));
    }

    @Override
    public List<Span> getAllSpans(Service service, long startTime) {
        return searchMapper.getAllSpans(service, startTime);
    }

    @Override
    public List<EndPointTraceIdVo> getEndPointTraceIdVos(EndPointQueryDto endPointQueryDto) {
        return searchMapper.getEndPointTraceIdVos(endPointQueryDto);
    }

    @Override
    public R getSpanNamesByServiceName(SpanNamesQueryDto spanNamesQueryDto) {
        return searchMapper.getSpanNamesByServiceName(spanNamesQueryDto);
    }

    @Override
    public R getSpanTimesBySpanName(SpanTimesQueryDto spanTimesQueryDto) {
        SpanTimesVo spanTimesVoList = searchMapper.getSpanTimesVoBySpanName(spanTimesQueryDto);
        return R.ok().put(RESULT, spanTimesVoList);
    }

    @Override
    public R getTimeRange() {
        return searchMapper.getTimeRange();
    }
}

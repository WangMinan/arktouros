package edu.npu.arktouros.service.otel.scheduled.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import co.elastic.clients.elasticsearch.indices.rollover.RolloverConditions;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.ElasticsearchConstants;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.service.otel.scheduled.ScheduledJob;
import edu.npu.arktouros.service.otel.search.SearchService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.npu.arktouros.service.otel.sinker.elasticsearch.ElasticsearchSinkService.getMappings;

/**
 * @author : [wangminan]
 * @description : Elasticsearch定时任务
 */
@Slf4j
public class ElasticsearchScheduledJob extends ScheduledJob {

    public ElasticsearchScheduledJob(SearchService searchService) {
        super(searchService);
    }

    @Override
    public void startJobs() {
        // 获取所有service
        List<Service> services = searchService.getAllServices();
        // 启动所有定时任务
        rollOverThreadPool.scheduleAtFixedRate(
                this::rollOver, 0,
                Integer.parseInt(PropertiesProvider.getProperty(
                        "elasticsearch.schedule.rollover",
                        "1")),
                TimeUnit.HOURS);
        for (Service service : services) {
            calculateThroughputThreadPool.scheduleAtFixedRate(
                    () -> calculateThroughput(service),
                    0,
                    Integer.parseInt(
                            PropertiesProvider.getProperty(
                                    "elasticsearch.schedule.throughput",
                                    "5")),
                    TimeUnit.MINUTES);
            calculateResponseTimeThreadPool.scheduleAtFixedRate(
                    () -> calculateResponseTime(service),
                    0,
                    Integer.parseInt(PropertiesProvider.getProperty(
                            "elasticsearch.schedule.responseTime",
                            "5")),
                    TimeUnit.MINUTES);
            calculateErrorRateThreadPool.scheduleAtFixedRate(
                    () -> calculateErrorRate(service),
                    0,
                    Integer.parseInt(PropertiesProvider.getProperty(
                            "elasticsearch.schedule.errorRate",
                            "5")),
                    TimeUnit.MINUTES);
        }
    }

    @Override
    protected void rollOver() {
        log.info("Rollover start.");
        for (String indexName : ElasticsearchIndex.getIndexList()) {
            try {
                handleRollOver(indexName);
            } catch (IOException e) {
                // 不抛异常 大不了我们就不分嘛
                log.error("Rollover index:{} error.", indexName, e);
            }
        }
        log.info("All rollover job complete.");
    }

    @Override
    protected void calculateThroughput(Service service) {
        log.info("Calculate throughput start for service:{}.", service.getName());
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        // 通过service name查询
        int traceCount = searchService.getTraceCount(service, startTime, endTime);
        // 3. 时间/时间=吞吐量 写入gauge
        Gauge throughput = Gauge.builder()
                .name("throughput")
                .description("Total request count per 5 minutes.")
                .value(traceCount / 5.0)
                .timestamp(endTime)
                .labels(new HashMap<>())
                .build();
        throughput.setServiceName(service.getName());
        try {
            ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(), throughput);
        } catch (IOException e) {
            log.error("Sink response time for service:{} error.",
                    service.getName(), e);
        }
        log.info("Calculate throughput for service:{} complete.", service.getName());
    }

    @Override
    protected void calculateResponseTime(Service service) {
        log.info("Calculate response time start for service:{}.", service.getName());
        // 重走一遍span树
        List<EndPointTraceIdVo> traceIdVos = searchService.getEndPointTraceIdVos(
                new EndPointQueryDto(service.getName(), 1, ElasticsearchConstants.MAX_PAGE_SIZE));
        List<String> traceIds = new ArrayList<>();
        traceIdVos.stream()
                .map(EndPointTraceIdVo::traceIds)
                .forEach(traceIds::addAll);
        List<Long> costTimes = new ArrayList<>();
        for (String traceId : traceIds) {
            List<SpanTreeNode> spanTreeNodeVos = searchService
                    .getSpanTreeInFiveMinutes(service.getName(),
                            traceId,
                            System.currentTimeMillis() - 5 * 60 * 1000,
                            System.currentTimeMillis());
            costTimes.addAll(spanTreeNodeVos.stream()
                    .map(this::getCostTimeForSpanTree)
                    .toList()
            );
        }
        // costTime求均值
        double avgCostTime = costTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        // 写入gauge
        Gauge responseTime = Gauge.builder()
                .name("response_time")
                .description("Average response time per 5 minutes.")
                .value(avgCostTime)
                .timestamp(System.currentTimeMillis())
                .labels(new HashMap<>())
                .build();
        responseTime.setServiceName(service.getName());
        try {
            ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    responseTime);
        } catch (IOException e) {
            log.error("Sink response time for service:{} error.",
                    service.getName(), e);
        }
        log.info("Calculate response time for service:{} complete.", service.getName());
    }

    @Override
    protected void calculateErrorRate(Service service) {
        log.info("Calculate error rate start for service:{}.", service.getName());
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        List<Span> allSpans = searchService.getAllSpans(service, startTime, endTime);
        // 3. 计算错误率
        long errorCount = allSpans.stream()
                .filter(span -> span.getEndTime() == PersistentDataConstants.ERROR_SPAN_END_TIME)
                .count();
        double value = allSpans.isEmpty() ? 0 : errorCount * 1.0 / allSpans.size();
        // 4. 写入gauge
        Gauge errorRate = Gauge.builder()
                .name("error_rate")
                .description("Error rate per 5 minutes.")
                .value(value)
                .timestamp(endTime)
                .labels(new HashMap<>())
                .build();
        errorRate.setServiceName(service.getName());
        try {
            ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    errorRate);
        } catch (IOException e) {
            log.error("Sink error rate for service:{} error.",
                    service.getName(), e);
        }
        log.info("Calculate error rate for service:{} complete.", service.getName());
    }

    /**
     * 递归计算span树的用时
     *
     * @param spanTreeNode 调用树
     * @return 当前层级及以下层级的最大时间开销
     */
    private Long getCostTimeForSpanTree(SpanTreeNode spanTreeNode) {
        // 不可达节点
        if (spanTreeNode.getSpan().getEndTime() ==
                PersistentDataConstants.ERROR_SPAN_END_TIME) {
            return 0L;
        }
        // 深度优先搜索
        if (spanTreeNode.getChildren().isEmpty()) {
            return spanTreeNode.getSpan().getEndTime() -
                    spanTreeNode.getSpan().getStartTime();
        }
        // 当前用时=当前span用时+下层Span用时
        return spanTreeNode.getSpan().getEndTime() -
                spanTreeNode.getSpan().getStartTime() +
                spanTreeNode.getChildren().stream()
                        .map(this::getCostTimeForSpanTree)
                        .max(Long::compareTo)
                        .orElse(0L);
    }

    public void handleRollOver(String indexName) throws IOException {
        RolloverRequest rolloverRequest = new RolloverRequest.Builder()
                .alias(indexName)
                .conditions(new RolloverConditions.Builder()
                        // 单页大小达到5gb时翻页
                        .maxSize(PropertiesProvider.getProperty(
                                "elasticsearch.rollover.maxSize", "5gb"))
                        .maxAge(new Time.Builder()
                                .time(PropertiesProvider.getProperty(
                                        "elasticsearch.rollover.maxAge", "1d"))
                                .build())
                        .maxDocs(Long.parseLong(PropertiesProvider.getProperty(
                                "elasticsearch.rollover.maxDocs", "100000")))
                        .build())
                // 要重新制定mappings 不然会出问题
                .mappings(getMappings(indexName))
                .build();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        RolloverResponse rolloverResponse = esClient.indices()
                .rollover(rolloverRequest);
        ElasticsearchClientPool.returnClient(esClient);
        if (!rolloverResponse.acknowledged()) {
            log.info("Check rollover for index:{}, nothing happened.", indexName);
        } else {
            log.info("Check rollover for index:{}, rollover complete, old index:{}, new index:{}.",
                    indexName, rolloverResponse.oldIndex(), rolloverResponse.newIndex());
        }
    }
}

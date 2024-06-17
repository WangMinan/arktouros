package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import co.elastic.clients.elasticsearch.indices.rollover.RolloverConditions;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.service.otel.search.SearchService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static edu.npu.arktouros.service.otel.sinker.elasticsearch.ElasticsearchSinkService.getMappings;

/**
 * @author : [wangminan]
 * @description : 所有定时任务 包括rollOver调用
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "instance.active",
        name = "searchMapper",
        havingValue = "elasticsearch")
public class ElasticsearchScheduledTasks {

    private static final String RESULT = "result";

    @Resource
    private SearchService searchService;

    private final ExecutorService calculateThroughputThreadPool =
            Executors.newCachedThreadPool(new BasicThreadFactory.Builder()
                    .namingPattern("Calculate-throughput-%d").build());

    private final ExecutorService calculateResponseTimeThreadPool =
            Executors.newCachedThreadPool(new BasicThreadFactory.Builder()
                    .namingPattern("Calculate-response-time-%d").build());

    private final ExecutorService calculateErrorRateThreadPool =
            Executors.newCachedThreadPool(new BasicThreadFactory.Builder()
                    .namingPattern("Calculate-error-rate-%d").build());

    // 一个小时执行一次
    @Scheduled(cron = "${elasticsearch.schedule.rollover}")
    public void tryRollover() {
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

    /**
     * FE 计算吞吐量
     */
    @Scheduled(cron = "${elasticsearch.schedule.throughput}")
    public void calculateThroughput() {
        // 1. 获取所有service
        List<Service> services = searchService.getAllServices();
        for (Service service : services) {
            log.info("Start calculate throughput for service:{}.", service.getName());
            calculateThroughputThreadPool.submit(() -> {
                try {
                    generateThroughputForService(service);
                } catch (IOException e) {
                    log.error("Generate throughput for service:{} error.",
                            service.getName(), e);
                }
            });
            log.info("Calculate throughput for service:{} complete.", service.getName());
        }
    }

    /**
     * FE 计算响应时间
     */
    @Scheduled(cron = "${elasticsearch.schedule.responseTime}")
    public void calculateResponseTime() {
        // 得把span树的那个逻辑全跑一遍 然后用span树做深度优先搜索 把一条trace的用时计算出来
        // 1. 获取所有service
        List<Service> services = searchService.getAllServices();
        for (Service service : services) {
            log.info("Start calculate response time for service:{}.", service.getName());
            calculateResponseTimeThreadPool.submit(() -> {
                generateResponseTimeForService(service);
            });
            log.info("Calculate response time for service:{} complete.", service.getName());
        }
    }

    /**
     * FE 计算错误率
     */
    @Scheduled(cron = "${elasticsearch.schedule.errorRate}")
    public void calculateErrorRate() {
        // 分析所有span，如果endTime为PersistentDataConstants.ERROR_SPAN_END_TIME则表示不可达
        // 1. 获取所有service
        List<Service> services = searchService.getAllServices();
        for (Service service : services) {
            log.info("Start calculate error rate for service:{}.", service.getName());
            calculateErrorRateThreadPool.submit(() -> {
                generateErrorRateForService(service);
            });
            log.info("Calculate error rate for service:{} complete.", service.getName());
        }
    }

    private void generateErrorRateForService(Service service) {
        // 2. 到span表对找到service在上一时间段内产生的所有Span
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        List<Span> allSpans = searchService.getAllSpans(service, startTime, endTime);
        // 3. 计算错误率
        long errorCount = allSpans.stream()
                .filter(span -> span.getEndTime() == PersistentDataConstants.ERROR_SPAN_END_TIME)
                .count();
        // 4. 写入gauge
        Gauge errorRate = Gauge.builder()
                .name("error_rate")
                .labels(Map.of("service_name", service.getName()))
                .description("Error rate per 5 minutes.")
                .value(errorCount * 1.0 / allSpans.size())
                .timestamp(endTime)
                .build();
        try {
            ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    errorRate);
        } catch (IOException e) {
            log.error("Sink error rate for service:{} error.",
                    service.getName(), e);
        }
    }

    private void generateResponseTimeForService(Service service) {
        // 重走一遍span树
        R endPointListByServiceName = searchService.getEndPointListByServiceName(
                new EndPointQueryDto(service.getName(), 1, Integer.MAX_VALUE));
        List<String> traceIds = new ArrayList<>();
        ((List<EndPointTraceIdVo>) endPointListByServiceName.get(RESULT))
                .stream()
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
                .labels(Map.of("service_name", service.getName()))
                .description("Average response time per 5 minutes.")
                .value(avgCostTime)
                .timestamp(System.currentTimeMillis())
                .build();
        try {
            ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    responseTime);
        } catch (IOException e) {
            log.error("Sink response time for service:{} error.",
                    service.getName(), e);
        }
    }

    /**
     * 递归计算span树的用时
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

    private void generateThroughputForService(Service service) throws IOException {
        // 2. 到span表对找到service在上一时间段内产生的trace数量 我们用traceId聚合计算
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        // 通过service name查询
        int traceCount = searchService.getTraceCount(service, startTime, endTime);
        // 3. 时间/数量=吞吐量 写入gauge
        Gauge throughput = Gauge.builder()
                .name("throughput")
                .labels(Map.of("service_name", service.getName()))
                .description("Total request count per 5 minutes.")
                .value(traceCount / 5.0)
                .timestamp(endTime)
                .build();
        ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(), throughput);
    }

    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 1000))
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

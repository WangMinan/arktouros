package edu.npu.arktouros.service.scheduled.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import co.elastic.clients.elasticsearch.indices.rollover.RolloverConditions;
import edu.npu.arktouros.model.common.ElasticsearchConstants;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.config.PropertiesProvider;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.service.search.SearchService;
import edu.npu.arktouros.service.sinker.SinkService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.retry.annotation.Retryable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static edu.npu.arktouros.service.sinker.elasticsearch.ElasticsearchSinkService.getMappings;

/**
 * @author : [wangminan]
 * @description : Elasticsearch定时任务
 */
@Slf4j
public class ElasticsearchScheduledJob extends ScheduledJob {

    public ElasticsearchScheduledJob(SearchService searchService, SinkService sinkService) {
        super(searchService, sinkService);
        log.info("ElasticsearchScheduledJob initialize complete.");
    }

    @Override
    public void start() {
        log.info("Starting all scheduled jobs.");
        // 启动所有定时任务
        rolloverThreadPool.scheduleAtFixedRate(
                this::rollover,
                // rollover不延迟
                0,
                Integer.parseInt(PropertiesProvider.getProperty(
                        "elasticsearch.schedule.rollover",
                        "1")),
                TimeUnit.HOURS);
        calculateThroughputThreadPool.scheduleAtFixedRate(
                this::calculateThroughput,
                // 随机0-10分钟
                (long) (Math.random() * 10),
                Integer.parseInt(
                        PropertiesProvider.getProperty(
                                "elasticsearch.schedule.throughput",
                                "5")),
                TimeUnit.MINUTES);
        calculateResponseTimeThreadPool.scheduleAtFixedRate(
                this::calculateResponseTime,
                // 随机0-10分钟
                (long) (Math.random() * 10),
                Integer.parseInt(PropertiesProvider.getProperty(
                        "elasticsearch.schedule.responseTime",
                        "5")),
                TimeUnit.MINUTES);
        calculateErrorRateThreadPool.scheduleAtFixedRate(
                this::calculateErrorRate,
                // 随机0-10分钟
                (long) (Math.random() * 10),
                Integer.parseInt(PropertiesProvider.getProperty(
                        "elasticsearch.schedule.errorRate",
                        "5")),
                TimeUnit.MINUTES);
//        simulateMetricThreadPool.scheduleAtFixedRate(
//                () -> simulateMetrics(services),
//                // 随机0-10分钟
//                (long) (Math.random() * 10),
//                Integer.parseInt(PropertiesProvider.getProperty(
//                        "elasticsearch.schedule.metric",
//                        "5")),
//                TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        log.info("Stopping all scheduled jobs.");
        rolloverThreadPool.shutdown();
        calculateThroughputThreadPool.shutdown();
        calculateResponseTimeThreadPool.shutdown();
        calculateErrorRateThreadPool.shutdown();
        simulateMetricThreadPool.shutdown();
    }

    @Override
    protected void rollover() {
        log.info("Rollover start.");
        for (String indexName : ElasticsearchIndex.getIndexList()) {
            try {
                log.info("Rollover index: {}", indexName);
                handleRollover(indexName);
            } catch (IOException e) {
                // 不抛异常 大不了我们就不分嘛
                log.error("Rollover index:{} error.", indexName, e);
            }
        }
        log.info("All rollover job complete.");
    }

    @Override
    protected void simulateMetrics() {
        // 获取所有service
        List<Service> services = searchService.getAllServices();
        services.forEach(service -> {
            // 开始制造数据
            log.info("Stimulate metrics start.");
            Random random = new Random();
            // 1. CPU usage
            Gauge cpuUsage = Gauge.builder()
                    .name("cpu_usage")
                    .description("Average CPU usage per %d minutes in percentage."
                            .formatted(Integer.parseInt(PropertiesProvider.getProperty(
                            "elasticsearch.schedule.metric",
                            "5"))))
                    // 0-10 小数点后1位
                    .value(random.nextInt(101) / 10.0)
                    .timestamp(System.currentTimeMillis())
                    .labels(new HashMap<>())
                    .build();
            cpuUsage.setServiceName(service.getName());
            // 2. Memory usage
            Gauge memoryUsage = Gauge.builder()
                    .name("memory_usage")
                    .description("Average memory usage per %s minutes in MB."
                            .formatted(Integer.parseInt(PropertiesProvider.getProperty(
                                            "elasticsearch.schedule.metric"))))
                    // 50-150 小数点后1位
                    .value(50 + (random.nextInt(1001) / 10.0))
                    .timestamp(System.currentTimeMillis())
                    .labels(new HashMap<>())
                    .build();
            memoryUsage.setServiceName(service.getName());
            try {
                ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                        cpuUsage);
                ElasticsearchUtil.sink(ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                        memoryUsage);
            } catch (IOException e) {
                log.error("Sink stimulate metrics for service:{} error.",
                        service.getName(), e);
            }
            log.info("Stimulate metrics for service:{} complete.", service.getName());
        });
    }

    @Override
    protected void calculateThroughput() {
        // 获取所有service
        List<Service> services = searchService.getAllServices();
        services.forEach(service -> {
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
        });
    }

    @Override
    protected void calculateResponseTime() {
        // 获取所有service
        List<Service> services = searchService.getAllServices();
        services.forEach(service -> {
            UpdateServiceGauge updateServiceGauge = sinkResponseTime(service);
            if (updateServiceGauge.isShouldUpdate()) {
                updateServiceLatency(service, updateServiceGauge.value);
            }
        });
    }

    private UpdateServiceGauge sinkResponseTime(Service service) {
        log.info("Calculate response time start for service:{}.", service.getName());
        // 重走一遍span树
        List<EndPointTraceIdVo> traceIdVos = searchService.getEndPointTraceIdVos(
                new EndPointQueryDto(service.getName(), 1, ElasticsearchConstants.MAX_PAGE_SIZE, 0L, Long.MAX_VALUE));
        List<String> traceIds = new ArrayList<>();
        traceIdVos.stream()
                .map(EndPointTraceIdVo::traceIds)
                .forEach(traceIds::addAll);
        List<Long> costTimes = new ArrayList<>();
        for (String traceId : traceIds) {
            List<SpanTreeNode> spanTreeNodeVos = searchService
                    .getSpanTreeInMinutes(service.getName(),
                            traceId,
                            System.currentTimeMillis() -
                                    (long) Integer.parseInt(PropertiesProvider.getProperty(
                                    "elasticsearch.schedule.errorRate",
                                    "5")) * 60 * 1000,
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
                .description("Average response time per %d minutes.".formatted(
                        Integer.parseInt(PropertiesProvider.getProperty(
                                "elasticsearch.schedule.errorRate",
                                "5"))))
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
        return new UpdateServiceGauge(!costTimes.isEmpty(), avgCostTime);
    }

    @Override
    protected void calculateErrorRate() {
        // 获取所有service
        List<Service> services = searchService.getAllServices();
        services.forEach(this::sinkErrorRate);
    }

    private void updateServiceStatus(Service service, double errorRate) {
        log.info("Start update service status: {}", service.getName());
        // 这是private方法 我们要做一个手动rollback
        Service sourceService = new Service();
        // 深拷贝
        BeanUtils.copyProperties(service, sourceService);
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .id(service.getId())
                .index(ElasticsearchIndex.SERVICE_INDEX.getIndexName())
                .build();
        boolean deleteResult = ElasticsearchUtil.delete(deleteRequest);
        if (deleteResult) {
            log.info("Delete source service success, inserting now.");
            try {
                service.setStatus(errorRate == 0);
                sinkService.sink(service);
            } catch (Exception e) {
                try {
                    // 手动回滚
                    rollbackUpdate(sourceService);
                } catch (IOException ex) {
                    log.error("Rollback update service:{} error. Need manual recover", service, ex);
                }
            }
        }
    }

    private void updateServiceLatency(Service service, double latency) {
        log.info("Start update service latency: {}", service.getName());
        // 这是private方法 我们要做一个手动rollback
        Service sourceService = new Service();
        // 深拷贝
        BeanUtils.copyProperties(service, sourceService);
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .id(service.getId())
                .index(ElasticsearchIndex.SERVICE_INDEX.getIndexName())
                .build();
        boolean deleteResult = ElasticsearchUtil.delete(deleteRequest);
        if (deleteResult) {
            log.info("Delete source service success, inserting now.");
            try {
                service.setLatency((int) latency);
                sinkService.sink(service);
            } catch (Exception e) {
                try {
                    // 手动回滚
                    rollbackUpdate(sourceService);
                } catch (IOException ex) {
                    log.error("Rollback update service:{} error. Need manual recover", service, ex);
                }
            }
        }
    }

    @Retryable(retryFor = {IOException.class}, maxAttempts = 3)
    private static void rollbackUpdate(Service service) throws IOException {
        ElasticsearchUtil.sink(ElasticsearchIndex.SERVICE_INDEX.getIndexName(), service);
    }

    private void sinkErrorRate(Service service) {
        log.info("Calculate error rate start for service:{}.", service.getName());
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        List<Span> allSpans = searchService.getAllSpans(service, startTime);
        // 3. 计算错误率
        long errorCount = allSpans.stream()
                .filter(span -> span.getEndTime() ==
                        PersistentDataConstants.ERROR_SPAN_END_TIME)
                .count();
        // 首先要重置tags
        List<Tag> tags = new ArrayList<>();
        service.setTags(tags);
        if (errorCount > 0) {
            allSpans.stream()
                    .filter(span -> span.getEndTime() ==
                            PersistentDataConstants.ERROR_SPAN_END_TIME)
                    .forEach(span -> tags.add(new Tag(
                            PersistentDataConstants.LATEST_ERROR_SPAN_ID, span.getId())));
            service.setTags(tags);
        }
        double value = allSpans.isEmpty() ? 0 : errorCount * 1.0 / allSpans.size();
        // 4. 写入gauge
        Gauge errorRate = Gauge.builder()
                .name("error_rate")
                .description("Error rate per %d minutes.".formatted(
                        Integer.parseInt(PropertiesProvider.getProperty(
                                "elasticsearch.schedule.errorRate",
                                "5"))))
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
        log.info("Calculate error rate for service:{} complete. Error rate: {}.", service.getName(), value);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UpdateServiceGauge {
        private boolean shouldUpdate;
        private double value;
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

    public void handleRollover(String indexName) throws IOException {
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

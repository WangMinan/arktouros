package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import co.elastic.clients.elasticsearch.indices.rollover.RolloverConditions;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
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

    @Resource
    private SearchService searchService;

    private final ExecutorService calculateThroughputThreadPool =
    Executors.newCachedThreadPool(new BasicThreadFactory.Builder()
            .namingPattern("Calculate-throughput-%d").build());

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

    @Scheduled(cron = "${elasticsearch.schedule.throughput}")
    public void tryCalculateThroughput() throws IOException {
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

    private void generateThroughputForService(Service service) throws IOException {
        // 2. 到span表对找到service在上一时间段内产生的span数量
        // 开始时间 头五分钟
        long startTime = System.currentTimeMillis() - 5 * 60 * 1000;
        // 结束时间 当前时间
        long endTime = System.currentTimeMillis();
        // 通过service name查询
        int spanCount = searchService.getSpanCount(service, startTime, endTime);
        // 3. 时间/数量=吞吐量 写入gauge
        Gauge throughput = Gauge.builder()
                .name("throughput")
                .labels(Map.of("service_name", service.getName()))
                .description("Total request count per 5 minutes.")
                .value((double) spanCount / 5)
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

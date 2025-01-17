package edu.npu.arktouros.service.scheduled;

import edu.npu.arktouros.service.search.SearchService;
import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 所有定时任务应该都在这里 覆盖了吞吐量，响应时间和错误率指标
 */
@Slf4j
public abstract class ScheduledJob {

    protected SearchService searchService;
    protected SinkService sinkService;

    // Elasticsearch主用 负责调用rollover api
    protected ScheduledExecutorService rolloverThreadPool;

    // 计算吞吐量
    protected ScheduledExecutorService calculateThroughputThreadPool;

    // 计算响应时间
    protected ScheduledExecutorService calculateResponseTimeThreadPool;

    // 计算错误率
    protected ScheduledExecutorService calculateErrorRateThreadPool;

    // 模拟数值数据
    protected ScheduledExecutorService simulateMetricThreadPool;

    public ScheduledJob(SearchService searchService, SinkService sinkService) {
        log.info("All scheduledJob init.");
        this.searchService = searchService;
        this.sinkService = sinkService;
        initThreadPools();
    }

    public abstract void start();

    public abstract void stop();

    public void flushAndStart() {
        log.info("Flush and start scheduled job.");
        initThreadPools();
        start();
    }

    protected void initThreadPools() {
        rolloverThreadPool =
                Executors.newScheduledThreadPool(10,
                        new BasicThreadFactory.Builder()
                                .namingPattern("Roll-over-%d").build());
        calculateThroughputThreadPool =
                Executors.newScheduledThreadPool(10,
                        new BasicThreadFactory.Builder()
                                .namingPattern("Calculate-throughput-%d").build());
        calculateResponseTimeThreadPool =
                Executors.newScheduledThreadPool(10,
                        new BasicThreadFactory.Builder()
                                .namingPattern("Calculate-response-time-%d").build());
        calculateErrorRateThreadPool =
                Executors.newScheduledThreadPool(10,
                        new BasicThreadFactory.Builder()
                                .namingPattern("Calculate-error-rate-%d").build());
        simulateMetricThreadPool =
                Executors.newScheduledThreadPool(10,
                        new BasicThreadFactory.Builder()
                                .namingPattern("Simulate-metric-%d").build());
    }

    protected abstract void rollover();

    protected abstract void simulateMetrics();

    protected abstract void calculateThroughput();

    protected abstract void calculateResponseTime();

    protected abstract void calculateErrorRate();
}

package edu.npu.arktouros.service.otel.scheduled;

import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.service.otel.search.SearchService;
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

    // Elasticsearch主用 负责调用rollover api
    protected final ScheduledExecutorService rollOverThreadPool =
            Executors.newScheduledThreadPool(10,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Roll-over-%d").build());

    // 计算吞吐量
    protected final ScheduledExecutorService calculateThroughputThreadPool =
            Executors.newScheduledThreadPool(10,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Calculate-throughput-%d").build());

    // 计算响应时间
    protected final ScheduledExecutorService calculateResponseTimeThreadPool =
            Executors.newScheduledThreadPool(10,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Calculate-response-time-%d").build());

    // 计算错误率
    protected final ScheduledExecutorService calculateErrorRateThreadPool =
            Executors.newScheduledThreadPool(10,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Calculate-error-rate-%d").build());

    // 模拟数值数据
    protected final ScheduledExecutorService simulateMetricThreadPool =
            Executors.newScheduledThreadPool(10,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Simulate-metrics-%d").build());

    public ScheduledJob(SearchService searchService) {
        log.info("All scheduledJob init.");
        this.searchService = searchService;
    }

    public abstract void startJobs();

    protected abstract void rollOver();

    protected abstract void simulateMetrics(Service service);

    protected abstract void calculateThroughput(Service service);

    protected abstract void calculateResponseTime(Service service);

    protected abstract void calculateErrorRate(Service service);
}

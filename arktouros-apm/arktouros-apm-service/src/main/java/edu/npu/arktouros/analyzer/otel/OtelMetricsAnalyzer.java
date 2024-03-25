package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import edu.npu.arktouros.service.otel.queue.MetricsQueueService;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : 数值分析模块
 */
@Component
@Slf4j
public class OtelMetricsAnalyzer extends DataAnalyzer {

    @Resource
    private MetricsQueueService queueService;

    public OtelMetricsAnalyzer() {
        this.setName("OtelMetricsAnalyzer");
    }

    public void handle(ResourceMetrics resourceMetrics) {
        try {
            String resourceMetricsJson = ProtoBufJsonUtils.toJSON(resourceMetrics);
            MetricsQueueItem metricsQueueItem = MetricsQueueItem.builder()
                    .data(resourceMetricsJson).build();
            queueService.put(metricsQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceMetrics:{} to json", resourceMetrics, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        log.info("OtelMetricsAnalyzer start to analyze data");
        while (!isInterrupted()) {
            analyze();
        }
    }

    public void analyze() {
        MetricsQueueItem item = queueService.get();
        if (item != null) {
            log.info("OtelMetricsAnalyzer start to analyze data");
        }
    }

    @Override
    public void interrupt() {
        log.info("OtelMetricsAnalyzer is shutting down.");
        super.interrupt();
    }
}

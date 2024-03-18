package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author : [wangminan]
 * @description : 数值分析模块
 */
@Slf4j
public class OtelMetricsAnalyzer extends DataAnalyzer {

    private final BlockingQueue<ResourceMetrics> queue;

    private static final int QUEUE_SIZE = 100;

    @Getter
    private static final OtelMetricsAnalyzer instance = new OtelMetricsAnalyzer();


    public OtelMetricsAnalyzer() {
        queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    public void handle(ResourceMetrics resourceMetrics) {
        try {
            queue.put(resourceMetrics);
        } catch (InterruptedException e) {
            log.error("Failed to put resourceMetrics:{} into queue", resourceMetrics, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        log.info("OtelMetricsAnalyzer start to analyze data");
        while (true) {
            analyze();
        }
    }

    public void analyze() {
        try {
            ResourceMetrics resourceMetrics = queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

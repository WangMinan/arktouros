package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author : [wangminan]
 * @description : 日志分析模块
 */
@Slf4j
public class OtelLogAnalyzer extends DataAnalyzer {

    private final BlockingQueue<ResourceLogs> queue;

    private static final int QUEUE_SIZE = 100;

    // 单例模式
    @Getter
    private static final OtelLogAnalyzer instance = new OtelLogAnalyzer();

    private OtelLogAnalyzer() {
        // 初始化
        queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    public void handle(ResourceLogs resourceLogs) {
        // 在新线程中进行分析
        try {
            queue.put(resourceLogs);
        } catch (InterruptedException e) {
            log.error("Failed to put resourceLogs:{} into queue", resourceLogs, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        log.info("OtelLogAnalyzer start to analyze data");
        while (true) {
            analyze();
        }
    }

    public void analyze() {
        try {
            ResourceLogs resourceLogs = queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

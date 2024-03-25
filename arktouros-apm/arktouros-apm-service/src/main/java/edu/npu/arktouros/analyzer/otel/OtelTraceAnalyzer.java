package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : 链路分析模块
 */
@Component
@Slf4j
public class OtelTraceAnalyzer extends DataAnalyzer {

    @Resource
    private TraceQueueService queueService;

    public OtelTraceAnalyzer() {
        this.setName("OtelTraceAnalyzer");
    }

    @Override
    public void run() {
        log.info("OtelTraceAnalyzer start to analyze data");
        while (!isInterrupted()) {
            analyze();
        }
    }

    public void handle(ResourceSpans resourceSpans) {
        try {
            String resourceSpansJson = ProtoBufJsonUtils.toJSON(resourceSpans);
            TraceQueueItem logQueueItem = TraceQueueItem.builder()
                    .data(resourceSpansJson).build();
            queueService.put(logQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceSpans:{} to json", resourceSpans, e);
            throw new RuntimeException(e);
        }
    }

    public void analyze() {
        TraceQueueItem item = queueService.get();
        if (item != null) {
            log.info("OtelTraceAnalyzer start to analyze data");
        }
    }

    @Override
    public void interrupt() {
        log.info("OtelTraceAnalyzer is shutting down.");
        super.interrupt();
    }

    public void sink() {

    }
}

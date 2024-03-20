package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : 日志分析模块
 */
@Slf4j
public class OtelLogAnalyzer extends DataAnalyzer {

    @Resource
    private LogQueueService logQueueService;

    // 单例模式
    @Getter
    private static final OtelLogAnalyzer instance = new OtelLogAnalyzer();

    private OtelLogAnalyzer() {
    }

    public void handle(ResourceLogs resourceLogs) {
        // 在新线程中进行分析
        try {
            String resourceLogsJson = ProtoBufJsonUtils.toJSON(resourceLogs);
            LogQueueItem logQueueItem = LogQueueItem.builder()
                    .data(resourceLogsJson).build();
            logQueueService.put(logQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceLogs:{} to json", resourceLogs, e);
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

    @Override
    public void interrupt() {
        super.interrupt();

    }

    public void analyze() {
        LogQueueItem item = logQueueService.get();
        if (item != null) {
            String data = item.getData();
            ResourceLogs.Builder builder = ResourceLogs.newBuilder();
            ResourceLogs resourceLogs = builder.build();
        }
    }
}

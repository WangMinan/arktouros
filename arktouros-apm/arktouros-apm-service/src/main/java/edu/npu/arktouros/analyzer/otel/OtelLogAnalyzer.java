package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.analyzer.otel.util.OtelAnalyzerUtil;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * @author : [wangminan]
 * @description : 日志分析模块
 */
@Slf4j
public class OtelLogAnalyzer extends DataAnalyzer {

    public static LogQueueService queueService;

    private final SinkService sinkService;

    public OtelLogAnalyzer(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    public static void handle(ResourceLogs resourceLogs) {
        // 在新线程中进行分析
        try {
            String resourceLogsJson = ProtoBufJsonUtils.toJSON(resourceLogs);
            LogQueueItem logQueueItem = LogQueueItem.builder()
                    .data(resourceLogsJson).build();
            queueService.put(logQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceLogs:{} to json", resourceLogs, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            transform();
        }
    }

    @Override
    public void interrupt() {
        log.info("OtelLogAnalyzer is shutting down.");
        super.interrupt();
    }

    public void transform() {
        log.info("OtelLogAnalyzer start to transform data");
        LogQueueItem item = queueService.get();
        if (item != null) {
            String json = item.getData();
            try {
                ResourceLogs.Builder builder = ResourceLogs.newBuilder();
                ProtoBufJsonUtils.fromJSON(json, builder);
                ResourceLogs resourceLogs = builder.build();
                io.opentelemetry.proto.resource.v1.Resource resource =
                        resourceLogs.getResource();
                Map<String, String> nodeLabels =
                        OtelAnalyzerUtil.convertAttributesToMap(resource.getAttributesList());
                for (ScopeLogs scopeLogs : resourceLogs.getScopeLogsList()) {
                    scopeLogs.getLogRecordsList().forEach(
                            logRecord -> {
                                Log sourceLog = Log.builder()
                                        .serviceName(nodeLabels.get("job_name"))
                                        .timestamp(TimeUnit.NANOSECONDS.toMillis(
                                                logRecord.getTimeUnixNano()))
                                        .content(logRecord.getBody().getStringValue())
                                        .spanId(OtelAnalyzerUtil.convertSpanId(logRecord.getSpanId()))
                                        .traceId(OtelAnalyzerUtil.convertSpanId(logRecord.getTraceId()))
                                        .severityText(logRecord.getSeverityText())
                                        .tags(logRecord
                                                .getAttributesList()
                                                .stream()
                                                .collect(toMap(KeyValue::getKey, this::buildTagValue))
                                                .entrySet()
                                                .stream()
                                                .map(it -> Tag.builder()
                                                        .key(it.getKey())
                                                        .value(it.getValue())
                                                        .build())
                                                .toList())
                                        .build();
                                try {
                                    sinkService.sink(sourceLog);
                                } catch (IOException e) {
                                    log.error("Failed to sink log after retry.", e);
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                }
            } catch (IOException e) {
                log.error("Failed to convert json:{} to resourceLogs", json, e);
            }

        }
    }

    private String buildTagValue(KeyValue it) {
        var value = it.getValue();
        if (value.hasStringValue()) return value.getStringValue();
        if (value.hasIntValue()) return String.valueOf(value.getIntValue());
        if (value.hasDoubleValue()) return String.valueOf(value.getDoubleValue());
        if (value.hasBoolValue()) return String.valueOf(value.getBoolValue());
        if (value.hasArrayValue()) return value.getArrayValue().toString();
        return "";
    }

    @Override
    public void init() {
        log.info("Initializing OtelLogAnalyzer:{} creating table APM_LOG_QUEUE.",
                this.getName());
        queueService.waitTableReady();
        log.info("OtelLogAnalyzer is ready.");
    }
}

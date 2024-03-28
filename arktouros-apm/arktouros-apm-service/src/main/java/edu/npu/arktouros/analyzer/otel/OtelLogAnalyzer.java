package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.analyzer.otel.util.OtelAnalyzerUtil;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * @author : [wangminan]
 * @description : 日志分析模块
 */
@Component
@Slf4j
public class OtelLogAnalyzer extends DataAnalyzer {

    @Resource
    private LogQueueService logQueueService;

    public OtelLogAnalyzer() {
        this.setName("OtelLogAnalyzer");
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
        log.info("OtelLogAnalyzer start to transform data");
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
        LogQueueItem item = logQueueService.get();
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
                                Log log = Log.builder()
                                        .name(nodeLabels.get("service_name"))
                                        .timestamp(logRecord.getTimeUnixNano() / 1_000_000)
                                        .content(logRecord.getBody().getStringValue())
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
                                                .collect(Collectors.toList()))
                                        .build();
                                // TODO emit log
                            }
                    );
                }
            } catch (IOException e) {
                log.error("Failed to convert json:{} to resourceLogs", json, e);
            }

        }
    }

    private String buildTagValue(KeyValue it) {
        final var value = it.getValue();
        return value.hasStringValue() ? value.getStringValue() :
                value.hasIntValue() ? String.valueOf(value.getIntValue()) :
                        value.hasDoubleValue() ? String.valueOf(value.getDoubleValue()) :
                                value.hasBoolValue() ? String.valueOf(value.getBoolValue()) :
                                        value.hasArrayValue() ? value.getArrayValue().toString() :
                                                "";
    }
}

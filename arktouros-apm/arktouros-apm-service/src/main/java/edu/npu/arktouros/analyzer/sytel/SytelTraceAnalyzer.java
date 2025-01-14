package edu.npu.arktouros.analyzer.sytel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 沈阳链路追踪遥测数据分析器
 */
@Slf4j
public class SytelTraceAnalyzer extends DataAnalyzer {

    protected static TraceQueueService queueService;

    private final SinkService sinkService;

    private final ObjectMapper objectMapper;

    public SytelTraceAnalyzer(SinkService sinkService, ObjectMapper objectMapper) {
        this.sinkService = sinkService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init() {
        log.info("Initializing SytelTraceAnalyzer:{}, creating table APM_TRACE_QUEUE.",
                this.getName());
        queueService.waitTableReady();
        log.info("OtelTraceAnalyzer is ready.");
    }

    @Override
    public void run() {
        super.run();
        if (!queueService.isEmpty()) {
            log.warn("Trace data has remained in cache. Error could occur if remained data is in otel format.");
        }
        while (!isInterrupted()) {
            transform();
        }
    }

    @Override
    public void interrupt() {
        log.info("Interrupting SytelTraceAnalyzer:{}", this.getName());
        if (!queueService.isEmpty() && needCleanWhileShutdown) {
            log.warn("Sytel trace data has remained in cache. Some data will be lost.");
            queueService.clear();
        }
        super.interrupt();
    }

    public static void setQueueService(TraceQueueService queueService) {
        SytelTraceAnalyzer.queueService = queueService;
    }

    public static void handle(String spanStr) {
        TraceQueueItem traceQueueItem = TraceQueueItem.builder()
                .data(spanStr)
                .build();
        queueService.put(traceQueueItem);
    }

    public void transform() {
        TraceQueueItem item = queueService.get();
        if (item != null && StringUtils.isNotEmpty(item.getData())) {
            log.info("SytelTraceAnalyzer start to transform data");
        } else {
            log.debug("SytelTraceAnalyzer get null data from queue, continue for next.");
            return;
        }
        try {
            edu.npu.arktouros.model.sytel.Span sytelSpan =
                    objectMapper.readValue(item.getData(), edu.npu.arktouros.model.sytel.Span.class);
            Span arktourosSpan = Span.builder()
                    .id(sytelSpan.getSpanId())
                    .traceId(sytelSpan.getSpanContext().getTraceId())
                    .parentSpanId(sytelSpan.getParentId())
                    .name(sytelSpan.getSpanId()) // 我们要拿这个spanId作为调用节点的名称了
                    .serviceName(sytelSpan.getOperationName())
                    .startTime(sytelSpan.getStartTime())
                    .endTime(sytelSpan.getFinishTime())
                    .root(sytelSpan.getParentId().isEmpty())
                    .localEndPoint(null)
                    .remoteEndPoint(null)
                    .tags(List.of(Tag.builder()
                            .key("duration")
                            .value(sytelSpan.getDuration().toString())
                            .build()))
                    .build();
            log.debug("Change sytel span to arktouros span: {}", arktourosSpan);
            sinkService.sink(arktourosSpan);
        } catch (JsonProcessingException e) {
            log.error("sytelSpan:{}", item.getData());
            throw new ArktourosException(e, "Failed to parse json to span data");
        } catch (IOException e) {
            throw new ArktourosException(e, "Failed to sink span data");
        }
    }
}

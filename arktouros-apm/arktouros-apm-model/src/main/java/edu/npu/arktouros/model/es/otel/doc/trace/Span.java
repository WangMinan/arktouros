package edu.npu.arktouros.model.es.otel.doc.trace;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.trace.v1.Span}
 */
@Data
@Builder
public class Span {
    private String traceId;
    private String spanId;
    private String traceState;
    private String parentSpanId;
    private String name;
    private int kind;
    private long startTimeUnixNano;
    private long endTimeUnixNano;
    private List<Map<String, Object>> attributes;
    private int droppedAttributesCount;
    private List<Event> events;
    private int droppedEventsCount;
    private List<Link> links;
    private int droppedLinksCount;
    private Status status;
}

package edu.npu.arktouros.model.es.otel.doc.trace;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.trace.v1.Span.Event}
 */
@Data
@Builder
public class Event {
    private long timeUnixNano;
    private String name;
    private List<Map<String, Object>> attributes;
    private int droppedAttributesCount;
}

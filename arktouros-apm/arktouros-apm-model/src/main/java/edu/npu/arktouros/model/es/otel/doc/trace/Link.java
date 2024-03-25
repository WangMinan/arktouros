package edu.npu.arktouros.model.es.otel.doc.trace;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.trace.v1.Span.Link}
 */
@Data
@Builder
public class Link {
    private String traceId;
    private String spanId;
    private String traceState;
    private List<Map<String, Object>> attributes;
    private int droppedAttributesCount;
}

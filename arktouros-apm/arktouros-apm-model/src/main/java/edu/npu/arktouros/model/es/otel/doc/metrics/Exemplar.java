package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Exemplar}
 */
@Data
@Builder
public class Exemplar {
    private List<Map<String, Object>> filteredAttributes;
    private long timeUnixNano;
    private double doubleValue;
    private int intValue;
    private String spanId;
    private String traceId;
}

package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.NumberDataPoint}
 */
@Data
@Builder
public class NumberDataPoint {

    private List<Map<String, Object>> attributes;
    private long startTimeUnixNano;
    private long timeUnixNano;
    private double doubleValue;
    private long longValue;
    private int flags;
    private List<Exemplar> exemplars;
}

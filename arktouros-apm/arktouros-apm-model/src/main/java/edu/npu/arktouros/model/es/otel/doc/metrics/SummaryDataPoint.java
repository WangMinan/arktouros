package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Summary}
 */
@Data
@Builder
public class SummaryDataPoint {
    private List<Map<String, Object>> attributes;
    private long startTimeUnixNano;
    private long timeUnixNano;
    private long count;
    private double sum;
    private List<ValueAtQuantile> quantileValues;
    private int flags;

    @Data
    @Builder
    static class ValueAtQuantile{
        private double quantile;
        private double value;
    }
}

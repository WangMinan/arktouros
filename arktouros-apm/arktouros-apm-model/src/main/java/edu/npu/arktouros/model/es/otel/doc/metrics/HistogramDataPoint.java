package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.HistogramDataPoint}
 */
@Data
@Builder
public class HistogramDataPoint {
    private List<Map<String, Object>> attributes;
    private long startTimeUnixNano;
    private long timeUnixNano;
    private long count;
    private double sum;
    private List<Long> bucketCounts;
    private List<Double> explicitBounds;
    private List<Exemplar> exemplars;
    private int flags;
    private double min;
    private double max;
}

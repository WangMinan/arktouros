package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.ExponentialHistogramDataPoint}
 */
@Data
@Builder
public class ExponentialHistogramDataPoint {
    private List<Map<String, Object>> attributes;
    private long startTimeUnixNano;
    private long timeUnixNano;
    private long count;
    private double sum;
    private int scale;
    private long zeroCount;
    private Bucket positive;
    private Bucket negative;
    private int flags;
    private double min;
    private double max;


    @Data
    @Builder
    static class Bucket {
        private int offset;
        private List<Long> bucketCounts;
    }
}

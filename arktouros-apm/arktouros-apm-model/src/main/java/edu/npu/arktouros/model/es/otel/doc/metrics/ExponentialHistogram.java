package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.ExponentialHistogram}
 */
@Data
@Builder
public class ExponentialHistogram {
    private List<ExponentialHistogramDataPoint> dataPoints;
    private int aggregationTemporality;
}

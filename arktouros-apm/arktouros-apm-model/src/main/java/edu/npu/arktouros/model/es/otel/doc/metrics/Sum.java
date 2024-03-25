package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Sum}
 */
@Data
@Builder
public class Sum {
    private List<NumberDataPoint> dataPoints;
    private boolean isMonotonic;
    private int aggregationTemporality;
}

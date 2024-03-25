package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Histogram} 直方图
 */
@Data
@Builder
public class Histogram {
    private List<HistogramDataPoint> dataPoints;
    private int aggregationTemporality;
}

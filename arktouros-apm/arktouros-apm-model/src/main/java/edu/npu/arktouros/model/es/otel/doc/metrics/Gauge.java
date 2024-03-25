package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Gauge}
 */
@Data
public class Gauge {
    List<NumberDataPoint> dataPoints;
}

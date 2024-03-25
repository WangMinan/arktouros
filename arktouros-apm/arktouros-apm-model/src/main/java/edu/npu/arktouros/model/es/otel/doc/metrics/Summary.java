package edu.npu.arktouros.model.es.otel.doc.metrics;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.Summary}
 */
@Data
@Builder
public class Summary {
    private List<SummaryDataPoint> dataPoints;
}

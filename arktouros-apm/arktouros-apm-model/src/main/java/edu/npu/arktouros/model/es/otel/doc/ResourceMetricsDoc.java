package edu.npu.arktouros.model.es.otel.doc;

import edu.npu.arktouros.model.es.otel.base.InstrumentationScope;
import edu.npu.arktouros.model.es.otel.base.Resource;
import edu.npu.arktouros.model.es.otel.doc.metrics.ExponentialHistogram;
import edu.npu.arktouros.model.es.otel.doc.metrics.Gauge;
import edu.npu.arktouros.model.es.otel.doc.metrics.Histogram;
import edu.npu.arktouros.model.es.otel.doc.metrics.Sum;
import edu.npu.arktouros.model.es.otel.doc.metrics.Summary;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.metrics.v1.ResourceMetrics} 在ES中的映射
 * 把enum等都简化了 只保留基本的属性 protobuf直接翻译出来的类是没办法用jackson来完成正常的序列化和反序列化的
 */
@Data
@AllArgsConstructor
@Builder
public class ResourceMetricsDoc {
    private Resource resource;
    private String schemaUrl;
    private List<ScopeMetric> scopeMetrics;

    public ResourceMetricsDoc(ResourceMetrics resourceMetrics) {

    }

    @Data
    @Builder
    static class ScopeMetric{
        private String schemaUrl;
        private InstrumentationScope scope;
        private List<Metric> metrics;

        @Data
        static class Metric{
            private String name;
            private String description;
            private String unit;
            private Gauge gauge;
            private Sum sum;
            private Histogram histogram;
            private ExponentialHistogram exponentialHistogram;
            private Summary summary;
        }
    }
}

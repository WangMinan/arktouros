package edu.npu.arktouros.model.otel.metric;

import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.EsProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import io.micrometer.common.util.StringUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : prometheus指标基类
 */
@Data
public abstract class Metric implements Source {
    protected String name;
    protected String serviceName;
    protected String description;
    protected Map<String, String> labels = new HashMap<>();
    protected long timestamp;
    protected final SourceType sourceType = SourceType.METRIC;
    protected MetricType metricType = MetricType.METRIC;

    public Metric(edu.npu.arktouros.proto.metric.v1.Metric metric) {
        this.name = metric.getName();
        this.description = metric.getDescription();
        this.labels = metric.getLabelsMap();
        this.timestamp = metric.getTimestamp();
        this.serviceName = metric.getServiceName();
    }

    private static final Map<String, Property> labelProperty =
            Map.of("key", Property.of(p ->
                            p.keyword(KeywordProperty.of(
                                    keywordProperty -> keywordProperty.index(true)))),
                    "value", Property.of(p ->
                            p.keyword(KeywordProperty.of(
                                    keywordProperty -> keywordProperty.index(true)))));

    protected static Map<String, Property> metricBaseMap = Map.of(
            "name",EsProperties.keywordIndexProperty,
            "serviceName", EsProperties.keywordIndexProperty,
            "description", Property.of(property ->
                    property.text(EsProperties.textKeywordProperty)),
            "labels", Property.of(property ->
                    property.nested(NestedProperty.of(
                            ne -> ne.properties(labelProperty)))),
            "timestamp", Property.of(property ->
                    property.date(DateProperty.of(
                            date -> date.index(true).format("epoch_millis")))),
            "sourceType", EsProperties.keywordIndexProperty,
            "metricType", EsProperties.keywordIndexProperty
    );

    protected Metric(String name, String description,
                     Map<String, String> labels, long timestamp) {
        this.name = name;
        this.description = description;
        this.labels = new HashMap<>(labels);
        this.timestamp = timestamp;
        if (StringUtils.isNotEmpty(labels.get("service_name"))) {
            this.serviceName = labels.get("service_name");
        } else if (name.startsWith("k8s.")) {
            this.serviceName = "k8s";
        }
    }

    public abstract Metric sum(Metric m);

    public abstract Double value();
}

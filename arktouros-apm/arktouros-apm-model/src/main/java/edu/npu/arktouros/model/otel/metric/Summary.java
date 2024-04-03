package edu.npu.arktouros.model.otel.metric;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : prometheus的Summary类型
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class Summary extends Metric {
    private long sampleCount;
    private double sampleSum;
    private final Map<Double, Double> quantiles;

    public static final Map<String, Property> documentMap = new HashMap<>();

    static {
        documentMap.putAll(metricBaseMap);
        documentMap.put("sampleCount", Property.of(property ->
                property.long_(longProperty ->
                        longProperty.index(true).store(true)))
        );
        documentMap.put("sampleSum", Property.of(property ->
                property.double_(doubleProperty ->
                        doubleProperty.index(true).store(true)))
        );
        documentMap.put("quantiles", Property.of(property ->
                property.nested(nestedProperty ->
                        nestedProperty.properties(Map.of(
                                "key", Property.of(p ->
                                        p.double_(doubleProperty ->
                                                doubleProperty.index(true).store(true))),
                                "value", Property.of(p ->
                                        p.double_(doubleProperty ->
                                                doubleProperty.index(true).store(true)))
                        ))
                ))
        );
    }

    @Builder
    public Summary(String name, @Singular Map<String, String> labels,
                   long sampleCount, double sampleSum,
                   @Singular Map<Double, Double> quantiles, long timestamp) {
        super(name, labels, timestamp);
        getLabels().remove("quantile");
        this.metricType = MetricType.SUMMARY;
        this.sampleCount = sampleCount;
        this.sampleSum = sampleSum;
        this.quantiles = quantiles;
    }

    @Override
    public Metric sum(Metric m) {
        Summary s = (Summary) m;
        this.sampleCount = this.sampleCount + s.getSampleCount();
        this.sampleSum = this.sampleSum + s.getSampleSum();
        return this;
    }

    @Override
    public Double value() {
        return this.getSampleSum() * 1000 / this.getSampleCount();
    }
}

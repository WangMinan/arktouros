package edu.npu.arktouros.model.es.metric;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

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

    @Builder
    public Summary(String name, @Singular Map<String, String> labels,
                   long sampleCount, double sampleSum,
                   @Singular Map<Double, Double> quantiles, long timestamp) {
        super(name, labels, timestamp);
        getLabels().remove("quantile");
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

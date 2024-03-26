package edu.npu.arktouros.model.es.metric;

import com.google.common.collect.Maps;
import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author : [wangminan]
 * @description : prometheus指标基类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class Metric extends Source {
    private final String serviceId;
    private final Map<String, String> labels;
    private final long timestamp;
    private final SourceType type = SourceType.METRIC;

    protected Metric(String name, String serviceId, Map<String, String> labels, long timestamp) {
        this.serviceId = serviceId;
        this.name = name;
        this.labels = Maps.newHashMap(labels);
        this.timestamp = timestamp;
    }

    public abstract Metric sum(Metric m);

    public abstract Double value();
}

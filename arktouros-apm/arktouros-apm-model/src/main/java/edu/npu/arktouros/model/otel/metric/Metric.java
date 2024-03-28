package edu.npu.arktouros.model.otel.metric;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : prometheus指标基类
 */
@Data
public abstract class Metric implements Source {
    private String name;
    // serviceId到底要怎么取是待定的
    private String serviceId;
    private String serviceName;
    private final Map<String, String> labels;
    private final long timestamp;
    private final SourceType type = SourceType.METRIC;

    protected Metric(String name, Map<String, String> labels, long timestamp) {
        this.name = name;
        this.labels = new HashMap<>(labels);
        this.serviceName = labels.get("service_name");
        this.timestamp = timestamp;
    }

    public abstract Metric sum(Metric m);

    public abstract Double value();
}

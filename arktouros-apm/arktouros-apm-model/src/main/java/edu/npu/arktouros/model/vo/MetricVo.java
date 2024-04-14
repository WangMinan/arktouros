package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.metric.MetricType;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : metric输出类型
 */
public record MetricVo(
        String name,
        String serviceName,
        MetricType metricType,
        List<Metric> metrics
) {
}

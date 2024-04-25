package edu.npu.arktouros.model.dto;

public record MetricQueryDto(
        String serviceName,
        Integer metricNameLimit,
        Long startTimeStamp,
        Long endTimeStamp
) {
}

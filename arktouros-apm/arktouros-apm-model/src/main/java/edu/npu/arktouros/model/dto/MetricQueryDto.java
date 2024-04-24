package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotEmpty;

public record MetricQueryDto(
        String serviceName,
        Integer metricNameLimit,
        Long startTimeStamp,
        Long endTimeStamp
) {
}

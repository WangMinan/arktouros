package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotEmpty;

public record MetricQueryDto(
        @NotEmpty(message = "serviceName should not be empty")
        String serviceName,
        Integer metricNameLimit,
        Long startTimeStamp,
        Long endTimeStamp
) {
}

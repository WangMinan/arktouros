package edu.npu.arktouros.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;

public record MetricQueryDto(
        @NotEmpty(message = "serviceName should not be empty")
        String serviceName,
        Integer metricNameLimit,
        @Past
        Long startTimeStamp,
        @PastOrPresent
        Long endTimeStamp
) {
}

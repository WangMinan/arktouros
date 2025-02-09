package edu.npu.arktouros.model.dto;


import jakarta.validation.constraints.NotBlank;

public record SpanTimesQueryDto(
        @NotBlank(message = "spanName should not be empty")
        String spanName,
        @NotBlank(message = "serviceName should not be empty")
        String serviceName,
        Long startTimestamp,
        Long endTimestamp
) {
}

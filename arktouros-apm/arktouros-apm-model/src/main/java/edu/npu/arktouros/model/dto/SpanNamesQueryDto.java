package edu.npu.arktouros.model.dto;


import jakarta.validation.constraints.NotBlank;

public record SpanNamesQueryDto(
        @NotBlank(message = "serviceName should not be empty")
        String serviceName,
        Long startTimestamp,
        Long endTimestamp
) {
}

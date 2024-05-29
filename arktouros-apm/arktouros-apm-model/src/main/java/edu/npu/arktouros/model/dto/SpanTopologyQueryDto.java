package edu.npu.arktouros.model.dto;


import jakarta.validation.constraints.NotNull;

public record SpanTopologyQueryDto(
        @NotNull(message = "traceId should not be empty")
        String traceId,
        @NotNull(message = "serviceName should not be empty")
        String serviceName
) {
}

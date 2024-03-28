package edu.npu.arktouros.model.otel.basic;

import lombok.Getter;

@Getter
public enum SourceType {
    NODE,
    SERVICE,
    TRACE,
    SPAN,
    LOG,
    METRIC,
    ENDPOINT;

    public static SourceType fromString(String type) {
        return SourceType.valueOf(type.toUpperCase());
    }

    public static String toString(SourceType type) {
        return type.toString().toLowerCase();
    }
}

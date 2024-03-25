package edu.npu.arktouros.model.es.arktouros.util;

import lombok.Getter;

@Getter
public enum SourceType {
    SERVICE,
    SPAN,
    LOG,
    METRICS;

    public static SourceType fromString(String type) {
        return SourceType.valueOf(type.toUpperCase());
    }

    public static String toString(SourceType type) {
        return type.toString().toLowerCase();
    }
}

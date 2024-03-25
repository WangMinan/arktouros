package edu.npu.arktouros.model.es.otel.doc.trace;

import lombok.Builder;
import lombok.Data;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.trace.v1.Status}
 */
@Data
@Builder
public class Status {
    private String message;
    private int code;
}

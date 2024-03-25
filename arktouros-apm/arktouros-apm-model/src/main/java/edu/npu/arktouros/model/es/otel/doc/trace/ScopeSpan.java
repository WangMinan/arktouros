package edu.npu.arktouros.model.es.otel.doc.trace;

import edu.npu.arktouros.model.es.otel.base.InstrumentationScope;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.trace.v1.ScopeSpans}
 */
@Data
@Builder
public class ScopeSpan {
    private InstrumentationScope scope;
    private String schemaUrl;
    private List<Span> spans;
}

package edu.npu.arktouros.model.es.otel.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.common.v1.InstrumentationScope}
 */
@Data
@AllArgsConstructor
@Builder
public class InstrumentationScope {

    private String name;
    private String version;
    private List<Map<String, Object>> attributes;
    private int droppedAttributesCount;

    public InstrumentationScope() {
        this.attributes = new ArrayList<>();
    }
}

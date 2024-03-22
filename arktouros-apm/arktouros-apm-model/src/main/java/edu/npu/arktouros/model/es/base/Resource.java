package edu.npu.arktouros.model.es.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.resource.v1.Resource}类的映射
 */
@Data
@AllArgsConstructor
@Builder
public class Resource {
    private List<Map<String, Object>> attributes;
    private int droppedAttributesCount;

    public Resource() {
        attributes = new ArrayList<>();
    }
}

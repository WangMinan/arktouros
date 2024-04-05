package edu.npu.arktouros.model.otel.structure;

import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.EsProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 一条链路的端点
 */
@Data
@Builder
public class EndPoint implements Source {
    private String serviceName;
    private String ip;
    private int port;
    private int latency;
    private SourceType type;

    public static final Map<String, Property> documentMap = new HashMap<>();
    static {
        documentMap.put("serviceName", Property.of(property ->
                property.text(EsProperties.keywordTextProperty)
        ));
        documentMap.put("ip", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("port", Property.of(property ->
                property.integer(IntegerNumberProperty.of(integerNumberProperty
                        -> integerNumberProperty.index(true))
                )
        ));
        documentMap.put("latency", Property.of(property ->
                property.integer(IntegerNumberProperty.of(integerNumberProperty
                        -> integerNumberProperty.index(true))
                )
        ));
        documentMap.put("type", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
    }
}

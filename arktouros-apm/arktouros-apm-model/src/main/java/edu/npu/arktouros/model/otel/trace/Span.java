package edu.npu.arktouros.model.otel.trace;

import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.EsProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 链路中的基础节点
 */
@Data
public class Span implements Source {
    private String name;
    private String id;
    private String serviceName;
    private String traceId;
    private String parentSpanId;
    private EndPoint localEndPoint;
    private EndPoint remoteEndPoint;
    private Long startTime;
    private Long endTime;
    private boolean root;
    private SourceType type = SourceType.SPAN;
    private List<Tag> tags;

    public static final Map<String, Property> documentMap = new HashMap<>();

    @Builder
    public Span(String id, String serviceName, String name, String traceId,
                String parentSpanId, EndPoint localEndPoint, EndPoint remoteEndPoint,
                Long startTime, Long endTime, boolean root, @Singular List<Tag> tags) {
        this.id = id;
        this.serviceName = serviceName;
        this.name = name;
        this.traceId = traceId;
        this.parentSpanId = parentSpanId;
        this.localEndPoint = localEndPoint;
        this.remoteEndPoint = remoteEndPoint;
        this.startTime = startTime;
        this.endTime = endTime;
        this.root = root;
        this.tags = tags;
    }

    static {
        documentMap.put("name", Property.of(property ->
                property.text(EsProperties.keywordTextProperty)
        ));
        documentMap.put("id", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("serviceName", Property.of(property ->
                property.text(EsProperties.keywordTextProperty)
        ));
        documentMap.put("traceId", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("parentSpanId", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("type", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("startTime", Property.of(property ->
                property.date(DateProperty.of(
                        date -> date.index(true).format("epoch_millis")))
        ));
        documentMap.put("endTime", Property.of(property ->
                property.date(DateProperty.of(
                        date -> date.index(true).format("epoch_millis")))
        ));
        documentMap.put("root", Property.of(property ->
                property.boolean_(BooleanProperty.of(
                        bool -> bool.index(true)))
        ));
        documentMap.put("localEndPoint", Property.of(property ->
                property.nested(nested -> nested.properties(EndPoint.documentMap))
        ));
        documentMap.put("remoteEndPoint", Property.of(property ->
                property.nested(nested -> nested.properties(EndPoint.documentMap))
        ));
        documentMap.put("tags", Property.of(property ->
                property.nested(nested -> nested.properties(Tag.documentMap))
        ));
    }
}

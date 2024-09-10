package edu.npu.arktouros.model.otel.trace;

import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.ElasticsearchProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 链路中的基础节点
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(callSuper = true)
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
    @Builder.Default
    private SourceType type = SourceType.SPAN;
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();

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

    public Span(edu.npu.arktouros.proto.span.v1.Span span) {
        this.id = span.getId();
        this.serviceName = span.getServiceName();
        this.name = span.getName();
        this.traceId = span.getTraceId();
        this.parentSpanId = span.getParentSpanId();
        this.localEndPoint = new EndPoint(span.getLocalEndPoint());
        this.remoteEndPoint = new EndPoint(span.getRemoteEndPoint());
        this.startTime = span.getStartTime();
        this.endTime = span.getEndTime();
        this.root = span.getRoot();
        this.type = SourceType.SPAN;
        span.getTagsList().forEach(tag -> this.tags.add(new Tag(tag)));
    }

    static {
        documentMap.put("name", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("id", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("serviceName", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("traceId", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("parentSpanId", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("type", ElasticsearchProperties.keywordIndexProperty);
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

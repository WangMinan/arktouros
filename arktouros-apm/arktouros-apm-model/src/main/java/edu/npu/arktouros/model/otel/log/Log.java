package edu.npu.arktouros.model.otel.log;

import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.EsProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 日志
 */
@Data
public class Log implements Source {
    private String serviceName;
    private String traceId;
    private String spanId;
    private SourceType type = SourceType.LOG;
    // 日志标准内容
    private String content;
    private List<Tag> tags = new ArrayList<>();
    private boolean error = false;
    private Long timestamp;

    public static final Map<String, Property> documentMap = new HashMap<>();

    @Builder
    public Log(
            String serviceName, Long timestamp,
            String spanId, String traceId,
            String content, @Singular List<Tag> tags, boolean error
    ) {
        this.serviceName = serviceName;
        this.timestamp = timestamp;
        this.spanId = spanId;
        this.traceId = traceId;
        this.content = content;
        this.tags = tags;
        this.error = error;
    }

    static {
        documentMap.put("serviceName", Property.of(property ->
                property.text(EsProperties.keywordTextProperty)
        ));
        documentMap.put("traceId", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("spanId", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("type", Property.of(property ->
                property.keyword(KeywordProperty.of(
                        keywordProperty -> keywordProperty.index(true)))
        ));
        documentMap.put("content", Property.of(property ->
                property.text(EsProperties.keywordTextProperty)
        ));
        documentMap.put("tags", Property.of(property ->
                property.nested(NestedProperty.of(
                        na -> na.properties(Tag.documentMap)))
        ));
        documentMap.put("error", Property.of(property ->
                property.boolean_(BooleanProperty.of(
                        booleanProperty -> booleanProperty.index(true)))
        ));
        documentMap.put("timestamp", Property.of(property ->
                property.date(DateProperty.of(
                        date -> date.index(true).format("epoch_millis")))
        ));
    }
}

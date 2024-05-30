package edu.npu.arktouros.model.otel.log;

import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.ElasticsearchProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 日志
 */
@Builder
@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString(callSuper = true)
public class Log implements Source {
    private String serviceName;
    private String traceId;
    private String spanId;
    @Builder.Default
    private SourceType type = SourceType.LOG;
    // 日志标准内容
    private String content;
    @Builder.Default
    private List<Tag> tags = new ArrayList<>();
    @Builder.Default
    private boolean error = false;
    private Long timestamp;
    @Builder.Default
    private String severityText = "Info"; // logLevel

    public static final Map<String, Property> documentMap = new HashMap<>();

    // 一定要有这个 Jackson找不到AllArgsConstructor生成的构造函数
    @JsonCreator
    public Log(@JsonProperty("serviceName") String serviceName,
               @JsonProperty("traceId") String traceId,
               @JsonProperty("spanId") String spanId,
               @JsonProperty("content") String content,
               @JsonProperty("tags") List<Tag> tags,
               @JsonProperty("error") boolean error,
               @JsonProperty("timestamp") Long timestamp,
               @JsonProperty("severityText") String severityText) {
        this.serviceName = serviceName;
        this.traceId = traceId;
        this.spanId = spanId;
        this.content = content;
        this.tags = tags;
        this.error = error;
        this.timestamp = timestamp;
        this.severityText = severityText;
    }

    // 不能用在类上的Builder注解，否则default设置会失效
    @Builder
    public Log(
            String serviceName, Long timestamp,
            String spanId, String traceId, String severityText,
            String content, @Singular List<Tag> tags, boolean error
    ) {
        this.serviceName = serviceName;
        this.timestamp = timestamp;
        this.spanId = spanId;
        this.traceId = traceId;
        this.content = content;
        this.tags = tags;
        this.error = error;
        this.severityText = severityText;
    }

    public Log(edu.npu.arktouros.proto.log.v1.Log log) {
        this.serviceName = log.getServiceName();
        this.traceId = log.getTraceId();
        this.spanId = log.getSpanId();
        this.type = SourceType.valueOf(log.getType().name());
        this.content = log.getContent();
        this.tags = new ArrayList<>();
        log.getTagsList().forEach(tag -> this.tags.add(new Tag(tag)));
        this.error = log.getError();
        this.timestamp = log.getTimestamp();
        this.severityText = log.getSeverityText();
    }

    static {
        documentMap.put("serviceName", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("traceId", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("spanId", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("type", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("content", Property.of(property ->
                property.text(ElasticsearchProperties.textKeywordProperty)
        ));
        documentMap.put("severityText", ElasticsearchProperties.keywordIndexProperty);
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

package edu.npu.arktouros.model.otel.structure;

import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.ElasticsearchProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 对标otel的resource 表示一个服务 每个trace应该包含多个service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "name"})
public class Service implements Source {
    private String id;
    private String name;
    private static final String DEFAULT_NAMESPACE = "default";
    private String nodeId;
    private SourceType type = SourceType.SERVICE;
    private String namespace;
    // GET_XXX; POST_XXX; RPC_XXX
    private String nodeName;
    private int latency;
    private int httpStatusCode;
    private String rpcStatusCode;
    // healthy 1; unhealthy 0
    private boolean status;
    // 留作扩展
    private List<Tag> tags = new ArrayList<>();

    public static final Map<String, Property> documentMap = new HashMap<>();

    static {
        documentMap.put("id", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("name", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("namespace", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("nodeId", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("type", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("nodeName", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("latency", Property.of(property ->
                property.integer(IntegerNumberProperty.of(
                        integerNumberProperty ->
                                integerNumberProperty.index(true).store(true)))));
        documentMap.put("httpStatusCode", Property.of(property ->
                property.integer(IntegerNumberProperty.of(
                        integerNumberProperty ->
                                integerNumberProperty.index(true).store(true)))));
        documentMap.put("rpcStatusCode", Property.of(property ->
                property.text(ElasticsearchProperties.textKeywordProperty)));
        documentMap.put("status", Property.of(property ->
                property.boolean_(BooleanProperty.of(
                        booleanProperty -> booleanProperty.index(true).store(true)))));
    }

    @Builder
    public Service(String name, boolean status) {
        this.namespace = DEFAULT_NAMESPACE;
        this.name = name;
        this.id = generateServiceId();
        this.status = status;
    }

    public Service(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
        this.id = generateServiceId();
    }

    private String generateServiceId() {
        String fullName = "service." + namespace + "." + name;
        return new String(
                Base64.getEncoder().encode(fullName.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
    }
}

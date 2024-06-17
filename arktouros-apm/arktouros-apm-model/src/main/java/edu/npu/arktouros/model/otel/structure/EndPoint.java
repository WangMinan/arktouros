package edu.npu.arktouros.model.otel.structure;

import co.elastic.clients.elasticsearch._types.mapping.IntegerNumberProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.ElasticsearchProperties;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 一条链路的端点
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EndPoint implements Source {
    private String serviceName;
    private String ip;
    private int port;
    private int latency;
    @Builder.Default
    private SourceType type = SourceType.ENDPOINT;

    @Builder
    public EndPoint(String serviceName, String ip, int port, int latency) {
        this.serviceName = serviceName;
        this.ip = ip;
        this.port = port;
        this.latency = latency;
    }

    public EndPoint(edu.npu.arktouros.proto.common.v1.EndPoint endPoint) {
        this.serviceName = endPoint.getServiceName();
        this.ip = endPoint.getIp();
        this.port = endPoint.getPort();
        this.latency = endPoint.getLatency();
        this.type = SourceType.ENDPOINT;
    }

    public static final Map<String, Property> documentMap = new HashMap<>();

    static {
        documentMap.put("serviceName", ElasticsearchProperties.keywordIndexProperty);
        documentMap.put("ip", ElasticsearchProperties.keywordIndexProperty);
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
        documentMap.put("type", ElasticsearchProperties.keywordIndexProperty);
    }
}

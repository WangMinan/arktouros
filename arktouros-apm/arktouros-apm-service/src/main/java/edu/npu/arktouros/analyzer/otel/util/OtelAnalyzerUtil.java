package edu.npu.arktouros.analyzer.otel.util;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.KeyValue;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : [wangminan]
 * @description : 用于otel分析器的工具类
 */
public class OtelAnalyzerUtil {
    private static final Map<String, String> LABEL_MAPPINGS =
            ImmutableMap
                    .<String, String>builder()
                    .put("net.host.name", "node_identifier_host_name")
                    .put("host.name", "node_identifier_host_name")
                    .put("job", "job_name")
                    .put("service.name", "job_name")
                    .build();

    public static Map<String, String> convertAttributesToMap(
            List<KeyValue> attributes) {
        return attributes.stream().collect(Collectors.toMap(
                it -> LABEL_MAPPINGS
                        .getOrDefault(it.getKey(), it.getKey())
                        .replaceAll("\\.", "_"),
                it -> it.getValue().getStringValue(),
                (v1, v2) -> v1
        ));
    }

    private Map<String, String> convertAttributeToMap(List<KeyValue> attrs) {
        return attrs.stream().collect(Collectors.toMap(
                KeyValue::getKey,
                attributeKeyValue -> attributeKeyValue.getValue().getStringValue(),
                (v1, v2) -> v1
        ));
    }

    public static String convertSpanId(ByteString spanId) {
        return ByteBuffer.wrap(spanId.toByteArray()).getLong() + "";
    }
}

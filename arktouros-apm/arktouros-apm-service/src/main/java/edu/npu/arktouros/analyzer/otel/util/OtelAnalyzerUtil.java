package edu.npu.arktouros.analyzer.otel.util;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.KeyValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : [wangminan]
 * @description : 用于otel分析器的工具类
 */
@Slf4j
public class OtelAnalyzerUtil {
    private OtelAnalyzerUtil() {
        throw new UnsupportedOperationException("OtelAnalyzerUtil is a utility class and cannot be instantiated");
    }

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

    public static String convertSpanId(ByteString spanId) {
        return convertSpanId(spanId, null);
    }

    public static String convertSpanId(ByteString spanId, String spanName) {
        try {
            return ByteBuffer.wrap(spanId.toByteArray()).getLong() + "";
        } catch (BufferUnderflowException e) {
            log.warn("Failed to convert spanId to long, spanId:{}, return null for default",
                    ByteBuffer.wrap(spanId.toByteArray()));
            if (StringUtils.isNotEmpty(spanName)) {
                return new String(
                        Base64.getEncoder().encode(spanName.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
            }
            return null;
        }
    }
}

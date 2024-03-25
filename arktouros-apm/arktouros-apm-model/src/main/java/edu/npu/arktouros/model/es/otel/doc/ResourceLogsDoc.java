package edu.npu.arktouros.model.es.otel.doc;

import edu.npu.arktouros.model.es.otel.base.InstrumentationScope;
import edu.npu.arktouros.model.es.otel.base.Resource;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : {@link io.opentelemetry.proto.logs.v1.ResourceLogs} 在ES中的映射
 * 把enum等都简化了 只保留基本的属性 protobuf直接翻译出来的类是没办法用jackson来完成正常的序列化和反序列化的
 */
@Data
@AllArgsConstructor
@Builder
public class ResourceLogsDoc {
    private Resource resource;
    private String schemaUrl;
    private List<ScopeLog> scopeLogs;

    public ResourceLogsDoc(ResourceLogs resourceLogs) {

    }

    @Data
    @Builder
    static class ScopeLog {
        private InstrumentationScope scope;
        private String schemaUrl;
        private List<LogRecord> logRecords;

        @Data
        @Builder
        static class LogRecord {
            private long timeUnixNano;
            private long observedTimeUnixNano;
            private int severityNumber;
            private String severityText;
            private String body;
            private List<Map<String,Object>> attributes;
            private int droppedAttributesCount;
            private int flags;
            private String traceId;
            private String spanId;
        }
    }
}

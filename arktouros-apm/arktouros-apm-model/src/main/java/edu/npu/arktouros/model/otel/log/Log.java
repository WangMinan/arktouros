package edu.npu.arktouros.model.otel.log;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import edu.npu.arktouros.model.otel.basic.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 日志
 */
@Data
public class Log implements Source {
    private String serviceId;
    private String serviceName;
    private String traceId;
    private String spanId;
    private SourceType type = SourceType.LOG;
    // 日志标准内容
    private String content;
    private List<Tag> tags = new ArrayList<>();
    private boolean error = false;
    private Long timestamp;

    @Builder
    public Log (
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
}

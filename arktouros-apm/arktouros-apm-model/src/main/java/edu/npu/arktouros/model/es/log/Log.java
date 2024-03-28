package edu.npu.arktouros.model.es.log;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import edu.npu.arktouros.model.es.basic.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Log extends Source {
    private String serviceId;
    private String serviceName;
    private String traceId;
    private int spanId;
    private SourceType type = SourceType.LOG;
    // 日志标准内容
    private String content;
    private List<Tag> tags = new ArrayList<>();
    private boolean error = false;
    private Long timestamp;

    @Builder
    public Log (
            String name, String serviceName, Long timestamp,
            String content, @Singular List<Tag> tags, boolean error
            ) {
        this.name = name;
        this.serviceName = serviceName;
        this.timestamp = timestamp;
        this.content = content;
        this.tags = tags;
        this.error = error;
    }
}

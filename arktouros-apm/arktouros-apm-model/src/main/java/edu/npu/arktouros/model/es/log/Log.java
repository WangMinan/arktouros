package edu.npu.arktouros.model.es.log;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import edu.npu.arktouros.model.es.basic.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    private String traceId;
    private int spanId;
    private SourceType type = SourceType.LOG;
    // 日志标准内容
    private String content;
    // 极少数二进制情况
    private byte[] tagsRawData;
    private List<Tag> tags = new ArrayList<>();
    private boolean error = false;
    private long timestamp;
}

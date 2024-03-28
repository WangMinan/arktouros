package edu.npu.arktouros.model.es.structure;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * @author : [wangminan]
 * @description : 一条链路的端点
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class EndPoint extends Source {
    private String serviceName;
    private int port;
    private int latency;
    private SourceType type = SourceType.ENDPOINT;
}

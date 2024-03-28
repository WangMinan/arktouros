package edu.npu.arktouros.model.otel.structure;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

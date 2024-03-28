package edu.npu.arktouros.model.otel.structure;

import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.basic.SourceType;
import lombok.Builder;
import lombok.Data;

/**
 * @author : [wangminan]
 * @description : 一条链路的端点
 */
@Data
@Builder
public class EndPoint implements Source {
    private String serviceName;
    private int port;
    private int latency;
    private SourceType type = SourceType.ENDPOINT;
}

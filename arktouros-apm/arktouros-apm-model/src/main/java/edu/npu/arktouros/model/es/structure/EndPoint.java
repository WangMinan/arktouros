package edu.npu.arktouros.model.es.structure;

import edu.npu.arktouros.model.es.Source;
import edu.npu.arktouros.model.es.basic.SourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author : [wangminan]
 * @description : 一条链路的端点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EndPoint extends Source {
    private String serviceId;
    private int latency;
    private SourceType type = SourceType.ENDPOINT;
}

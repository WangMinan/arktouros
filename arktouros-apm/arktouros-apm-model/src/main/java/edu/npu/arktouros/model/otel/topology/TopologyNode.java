package edu.npu.arktouros.model.otel.topology;

import lombok.Builder;
import lombok.Data;

/**
 * @author : [wangminan]
 * @description : 拓扑图节点
 */
@Data
@Builder
public class TopologyNode <T> {
    private T nodeObject;

    @Builder
    public TopologyNode(T nodeObject) {
        this.nodeObject = nodeObject;
    }
}

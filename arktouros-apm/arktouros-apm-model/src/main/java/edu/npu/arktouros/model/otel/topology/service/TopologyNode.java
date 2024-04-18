package edu.npu.arktouros.model.otel.topology.service;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author : [wangminan]
 * @description : 拓扑图节点
 */
@Data
@Builder
@EqualsAndHashCode(of = "nodeObject")
public class TopologyNode <T> {
    private T nodeObject;

    @Builder
    public TopologyNode(T nodeObject) {
        this.nodeObject = nodeObject;
    }
}

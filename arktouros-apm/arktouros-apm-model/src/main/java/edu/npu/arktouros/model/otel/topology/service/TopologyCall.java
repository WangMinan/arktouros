package edu.npu.arktouros.model.otel.topology.service;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author : [wangminan]
 * @description : 拓扑图调用
 */
@Data
@Builder
@EqualsAndHashCode(of = {"source", "target"})
public class TopologyCall <T> {
    private TopologyNode<T> source;
    private TopologyNode<T> target;

    @Builder
    public TopologyCall(TopologyNode<T> source, TopologyNode<T> target) {
        this.source = source;
        this.target = target;
    }
}

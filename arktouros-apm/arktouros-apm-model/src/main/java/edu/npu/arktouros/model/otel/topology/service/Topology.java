package edu.npu.arktouros.model.otel.topology.service;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.HashSet;
import java.util.Set;

/**
 * @author : [wangminan]
 * @description : 拓扑图的抽象类
 */
@Data
@NoArgsConstructor
@Builder
public class Topology<T> {
    @Builder.Default
    private Set<TopologyNode<T>> nodes = new HashSet<>();
    @Builder.Default
    private Set<TopologyCall<T>> calls = new HashSet<>();

    @Builder
    public Topology(@Singular Set<TopologyNode<T>> nodes,
                    @Singular Set<TopologyCall<T>> calls) {
        this.nodes = nodes;
        this.calls = calls;
    }
}

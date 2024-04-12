package edu.npu.arktouros.model.otel.topology;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 拓扑图的抽象类
 */
@Data
@NoArgsConstructor
@Builder
public class Topology<T> {
    @Builder.Default
    private List<TopologyNode<T>> nodes = new ArrayList<>();
    @Builder.Default
    private List<TopologyCall<T>> calls = new ArrayList<>();

    @Builder
    public Topology(@Singular List<TopologyNode<T>> nodes,
                    @Singular List<TopologyCall<T>> calls) {
        this.nodes = nodes;
        this.calls = calls;
    }
}

package edu.npu.arktouros.model.otel.topology;

import edu.npu.arktouros.model.otel.structure.Service;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 拓扑节点
 */
@Data
public class TopologyNode {
    private Service service;
    private List<TopologyNode> parentNodes = new ArrayList<>();
    private List<TopologyNode> childNodes = new ArrayList<>();

    @Builder
    public TopologyNode(Service service,
                        @Singular List<TopologyNode> parentNodes,
                        @Singular List<TopologyNode> childNodes) {
        this.service = service;
        this.parentNodes = parentNodes;
        this.childNodes = childNodes;
    }
}

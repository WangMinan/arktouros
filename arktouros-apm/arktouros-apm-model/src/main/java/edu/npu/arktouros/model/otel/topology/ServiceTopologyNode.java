package edu.npu.arktouros.model.otel.topology;

import edu.npu.arktouros.model.otel.structure.Service;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 拓扑节点
 */
@Data
@NoArgsConstructor
public class ServiceTopologyNode {
    private Service service;
    private List<ServiceTopologyNode> parentNodes = new ArrayList<>();
    private List<ServiceTopologyNode> childNodes = new ArrayList<>();

    @Builder
    public ServiceTopologyNode(Service service,
                               @Singular List<ServiceTopologyNode> parentNodes,
                               @Singular List<ServiceTopologyNode> childNodes) {
        this.service = service;
        this.parentNodes = parentNodes;
        this.childNodes = childNodes;
    }
}

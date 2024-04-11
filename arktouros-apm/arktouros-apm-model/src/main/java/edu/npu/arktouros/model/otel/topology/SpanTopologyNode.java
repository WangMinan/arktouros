package edu.npu.arktouros.model.otel.topology;

import edu.npu.arktouros.model.otel.trace.Span;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : Span的拓扑节点
 */
@Data
@NoArgsConstructor
public class SpanTopologyNode {
    private Span span;
    private List<SpanTopologyNode> parentNodes = new ArrayList<>();
    private List<SpanTopologyNode> childNodes = new ArrayList<>();

    @Builder
    public SpanTopologyNode(Span span,
                            @Singular List<SpanTopologyNode> parentNodes,
                            @Singular List<SpanTopologyNode> childNodes) {
        this.span = span;
        this.parentNodes = parentNodes;
        this.childNodes = childNodes;
    }
}

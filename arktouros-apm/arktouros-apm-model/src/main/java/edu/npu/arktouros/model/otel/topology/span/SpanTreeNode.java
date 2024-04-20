package edu.npu.arktouros.model.otel.topology.span;

import edu.npu.arktouros.model.otel.trace.Span;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : Span树上的节点 完全贴合 <a href="https://echarts.apache.org/zh/option.html#series-tree.data">...</a> 的指导结构
 */
@Builder
@Data
public class SpanTreeNode {
    private Span span;
    @Builder.Default
    private List<SpanTreeNode> children = new ArrayList<>();

    @Builder
    public SpanTreeNode(Span span, @Singular List<SpanTreeNode> children) {
        this.span = span;
        this.children = children;
    }
}

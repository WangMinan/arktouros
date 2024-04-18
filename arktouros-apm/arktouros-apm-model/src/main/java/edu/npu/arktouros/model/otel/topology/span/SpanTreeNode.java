package edu.npu.arktouros.model.otel.topology.span;

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
    private String name;
    private String value;
    @Builder.Default
    private boolean collapsed = false;
    @Builder.Default
    private List<SpanTreeNode> children = new ArrayList<>();

    @Builder
    public SpanTreeNode(String name, String value,
                        boolean collapsed, @Singular List<SpanTreeNode> children) {
        this.name = name;
        this.value = value;
        this.collapsed = collapsed;
        this.children = children;
    }
}

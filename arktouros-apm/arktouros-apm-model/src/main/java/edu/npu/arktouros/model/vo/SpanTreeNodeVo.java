package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link edu.npu.arktouros.model.otel.topology.span.SpanTreeNode} 对应的VO
 */
@Builder
@Data
public class SpanTreeNodeVo {
    private String name;
    private int value;
    @Builder.Default
    private boolean collapsed = false;
    @Builder.Default
    private List<SpanTreeNodeVo> children = new ArrayList<>();

    @Builder
    public SpanTreeNodeVo(String name, int value,
                        boolean collapsed, @Singular List<SpanTreeNodeVo> children) {
        this.name = name;
        this.value = value;
        this.collapsed = collapsed;
        this.children = children;
    }

    // 直接递归
    public SpanTreeNodeVo(SpanTreeNode spanTreeNode) {
        this.name = spanTreeNode.getSpan().getName();
        // value我暂时还没想好怎么转
        this.children = new ArrayList<>();
        for (SpanTreeNode child : spanTreeNode.getChildren()) {
            this.children.add(new SpanTreeNodeVo(child));
        }
    }
}

package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static edu.npu.arktouros.model.common.DiagramColor.COLOR_ERROR_RED_RGB;
import static edu.npu.arktouros.model.common.DiagramColor.COLOR_ERROR_YELLOW_RGB;
import static edu.npu.arktouros.model.common.DiagramColor.COLOR_NORMAL_GREEN_RGB;

/**
 * @author : [wangminan]
 * @description : {@link edu.npu.arktouros.model.otel.topology.span.SpanTreeNode} 对应的VO
 */
@Data
public class SpanTreeNodeVo {
    private String name;
    private boolean collapsed = false;
    private List<SpanTreeNodeVo> children;
    private ItemStyle itemStyle;
    // 我要加这个字段 不然前端看不出来span的实时状态
    private Span span;

    // 直接递归
    public SpanTreeNodeVo(SpanTreeNode spanTreeNode) {
        this.span = spanTreeNode.getSpan();
        this.name = spanTreeNode.getSpan().getName();
        if (spanTreeNode.getSpan().getEndTime() ==
                PersistentDataConstants.ERROR_SPAN_END_TIME ||
                (spanTreeNode.getSpan().getTags().stream().anyMatch(tag ->
                        tag.getKey().equals(Span.SpanTagKey.LONG_DURATION.getKey()) &&
                                tag.getValue().equals(Span.SpanTagValue.TRUE.getValue())))) {
            // 橙红色
            this.itemStyle = ItemStyle.builder()
                    .color(COLOR_ERROR_YELLOW_RGB.getColor())
                    .borderColor(COLOR_ERROR_RED_RGB.getColor())
                    .borderType("dashed")
                    .build();
        } else {
            // 绿色
            this.itemStyle = ItemStyle.builder()
                    .color(COLOR_NORMAL_GREEN_RGB.getColor())
                    .borderColor(COLOR_NORMAL_GREEN_RGB.getColor())
                    .borderType("solid")
                    .build();
        }
        // value我暂时还没想好怎么转
        this.children = new ArrayList<>();
        for (SpanTreeNode child : spanTreeNode.getChildren()) {
            this.children.add(new SpanTreeNodeVo(child));
        }
    }

    @Data
    @Builder
    private static class ItemStyle {
        private String color;
        private String borderColor;
        private String borderType;
    }
}

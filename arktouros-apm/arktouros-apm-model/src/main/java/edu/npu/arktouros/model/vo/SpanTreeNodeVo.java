package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : {@link edu.npu.arktouros.model.otel.topology.span.SpanTreeNode} 对应的VO
 */
@Data
public class SpanTreeNodeVo {
    private String name;
    private boolean collapsed = false;
    private List<SpanTreeNodeVo> children = new ArrayList<>();
    private ItemStyle itemStyle;
    // 我要加这个字段 不然前端看不出来span的实时状态
    private Span span;
    private int spanTimeout; 

    private static final String COLOR_ERROR_RED_RGB = "#FF2700";
    private static final String COLOR_ERROR_YELLOW_RGB = "#FFEE00";
    private static final String COLOR_NORMAL_GREEN_RGB = "#6EF780";

    // 直接递归
    public SpanTreeNodeVo(SpanTreeNode spanTreeNode) {
        this.spanTimeout = Integer.parseInt(PropertiesProvider.getProperty("sinker.span.timeout","5000")); 
        this.span = spanTreeNode.getSpan();
        this.name = spanTreeNode.getSpan().getName();
        if (spanTreeNode.getSpan().getEndTime() == PersistentDataConstants.ERROR_SPAN_END_TIME || 
            spanTreeNode.getSpan().getEndTime() - spanTreeNode.getSpan().getStartTime() > spanTimeout) {
            // 橙红色
            this.itemStyle = ItemStyle.builder()
                    .color(COLOR_ERROR_YELLOW_RGB)
                    .borderColor(COLOR_ERROR_RED_RGB)
                    .borderType("dashed")
                    .build();
        } else {
            // 绿色
            this.itemStyle = ItemStyle.builder()
                    .color(COLOR_NORMAL_GREEN_RGB)
                    .borderColor(COLOR_NORMAL_GREEN_RGB)
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

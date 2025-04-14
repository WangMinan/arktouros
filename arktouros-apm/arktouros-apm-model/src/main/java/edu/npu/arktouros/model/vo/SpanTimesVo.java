package edu.npu.arktouros.model.vo;

import edu.npu.arktouros.model.otel.trace.Span;
import lombok.Builder;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : [SpanTimout折线图用的VO，填充到echarts的data字段]
 */
@Data
public class SpanTimesVo {
    // 横坐标 所有时间戳
    private List<String> xAxis;
    private List<SpanTimesValue> spanTimesValues;

    public SpanTimesVo() {
        xAxis = new ArrayList<>();
        spanTimesValues = new ArrayList<>();
    }

    public void addSpan(Span span) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
                .format(span.getStartTime());
        xAxis.add(timestamp);
        SpanTimesValue spanTimesValue = SpanTimesValue.builder()
                .value(span.getEndTime() - span.getStartTime())
                .span(span)
                .build();
        spanTimesValues.add(spanTimesValue);
    }

    @Data
    @Builder
    private static class SpanTimesValue {
        private Long value;
        private Span span;
    }
}


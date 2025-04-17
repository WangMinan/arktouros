package edu.npu.arktouros.util;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.SpanTimesQueryDto;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.trace.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * @description  : [一句话描述该类的功能]
 * @author       : [wangminan]
 */
public class DurationUtil {
    /**
     * 使用均方差统计标记长时间跨度的span
     *
     * @param mapper mapper
     * @param currentSpan 当前span
     */
    public static void markLongDurationSpans(SearchMapper mapper, Span currentSpan) {
        // 对当前Span做均方差统计
        List<Span> spansWithSameName = mapper.getSpanListBySpanNameAndServiceName(
                new SpanTimesQueryDto(currentSpan.getName(), currentSpan.getServiceName(), null, null));
        if (spansWithSameName.isEmpty()) {
            return;
        }

        // Step 1: Compute mean duration
        double sum = 0;
        for (Span span : spansWithSameName) {
            sum += span.getEndTime() - span.getStartTime();
        }
        double mean = sum / spansWithSameName.size();

        // Step 2: Compute standard deviation
        double varianceSum = 0;
        for (Span span : spansWithSameName) {
            double diff = span.getEndTime() - span.getStartTime() - mean;
            varianceSum += diff * diff;
        }
        double standardDeviation = Math.sqrt(varianceSum / spansWithSameName.size());

        // Step 3: Mark spansWithSameName that deviate significantly (e.g., > mean + 2 * stddev)
        if (currentSpan.getTags() == null) {
            currentSpan.setTags(new ArrayList<>());
        }

        double duration = currentSpan.getEndTime() - currentSpan.getStartTime();
        if (duration > mean + 2 * standardDeviation) {
            // 打tag
            currentSpan.getTags().add(new Tag(
                    Span.SpanTagKey.LONG_DURATION.getKey(),
                    Span.SpanTagValue.TRUE.getValue()
            ));
        }
    }
}

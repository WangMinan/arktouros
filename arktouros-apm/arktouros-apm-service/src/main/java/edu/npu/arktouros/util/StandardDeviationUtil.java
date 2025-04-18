package edu.npu.arktouros.util;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.dto.SpanTimesQueryDto;
import edu.npu.arktouros.model.otel.basic.Tag;
import edu.npu.arktouros.model.otel.trace.Span;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StandardDeviationUtil {

    private static final Map<String, SpanStatistics> statsCache = new ConcurrentHashMap<>();

    private record SpanStatistics(double mean, double stdDev) {
    }

    public static void markLongDurationSpans(SearchMapper mapper, Span currentSpan) {
        if (currentSpan == null) return;

        String cacheKey = currentSpan.getServiceName() + ":" + currentSpan.getName();
        SpanStatistics stats = statsCache.computeIfAbsent(cacheKey, key -> calculateStats(mapper, currentSpan));

        if (stats == null) return;

        double duration = currentSpan.getEndTime() - currentSpan.getStartTime();
        if (duration > stats.mean + 2 * stats.stdDev) {
            addTag(currentSpan, Span.SpanTagKey.LONG_DURATION.getKey(), Span.SpanTagValue.TRUE.getValue());
        }
    }

    /**
     * Batch process spans to mark long duration spans.
     *
     * @param mapper search mapper
     * @param spans  list of spans
     */
    public static void markLongDurationSpansBatch(SearchMapper mapper, List<Span> spans) {
        if (spans == null || spans.isEmpty()) {
            return;
        }

        Map<String, List<Span>> groupedSpans = groupSpansByServiceAndName(spans);

        groupedSpans.forEach((key, spanGroup) -> {
            if (spanGroup.isEmpty()) {
                return;
            }

            Span firstSpan = spanGroup.getFirst();
            SpanStatistics stats = statsCache.computeIfAbsent(key, k -> calculateStats(mapper, firstSpan));

            if (stats == null) {
                return;
            }

            for (Span span : spanGroup) {
                // 均方差
                double duration = span.getEndTime() - span.getStartTime();
                // 如果当前span的持续时间大于均值+2倍标准差，则标记为长时间span
                if (duration > stats.mean + 2 * stats.stdDev) {
                    addTag(span, Span.SpanTagKey.LONG_DURATION.getKey(), Span.SpanTagValue.TRUE.getValue());
                }
            }
        });
    }

    /**
     * 计算均值和标准差
     *
     * @param mapper search mapper
     * @param span   当前span
     * @return 均值和标准差
     */
    private static SpanStatistics calculateStats(SearchMapper mapper, Span span) {
        List<Span> spans = mapper.getSpanListBySpanNameAndServiceName(
                new SpanTimesQueryDto(span.getName(), span.getServiceName(), null, null));

        if (spans.isEmpty()) {
            return null;
        }

        double mean = spans.stream().mapToDouble(s -> s.getEndTime() - s.getStartTime()).average().orElse(0);
        double stdDev = Math.sqrt(spans.stream()
                .mapToDouble(s -> Math.pow((s.getEndTime() - s.getStartTime()) - mean, 2))
                .average().orElse(0));

        return new SpanStatistics(mean, stdDev);
    }

    /**
     * 将 spans 按照服务名和 span 名称进行分组
     *
     * @param spans span列表
     * @return 分组后的 span 列表
     */
    private static Map<String, List<Span>> groupSpansByServiceAndName(List<Span> spans) {
        Map<String, List<Span>> groupedSpans = new HashMap<>();
        for (Span span : spans) {
            if (span != null) {
                String key = span.getServiceName() + ":" + span.getName();
                groupedSpans.computeIfAbsent(key, k -> new ArrayList<>()).add(span);
            }
        }
        return groupedSpans;
    }

    private static void addTag(Span span, String key, String value) {
        if (span.getTags() == null) {
            span.setTags(new ArrayList<>());
        }
        span.getTags().add(new Tag(key, value));
    }
}

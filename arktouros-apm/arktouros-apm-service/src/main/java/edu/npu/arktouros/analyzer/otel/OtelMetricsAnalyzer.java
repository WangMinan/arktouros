package edu.npu.arktouros.analyzer.otel;

import edu.npu.arktouros.analyzer.DataAnalyzer;
import edu.npu.arktouros.analyzer.otel.util.OtelAnalyzerUtil;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.sinker.SinkService;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.metrics.v1.SummaryDataPoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.opentelemetry.proto.metrics.v1.AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA;
import static io.opentelemetry.proto.metrics.v1.AggregationTemporality.AGGREGATION_TEMPORALITY_UNSPECIFIED;
import static java.util.stream.Collectors.toMap;

/**
 * @author : [wangminan]
 * @description : 数值分析模块
 */
@Slf4j
public class OtelMetricsAnalyzer extends DataAnalyzer {

    protected static MetricsQueueService queueService;

    private final SinkService sinkService;

    public OtelMetricsAnalyzer(SinkService sinkService) {
        this.sinkService = sinkService;
    }

    public static void handle(ResourceMetrics resourceMetrics) {
        try {
            String resourceMetricsJson = ProtoBufJsonUtils.toJSON(resourceMetrics);
            MetricsQueueItem metricsQueueItem = MetricsQueueItem.builder()
                    .data(resourceMetricsJson)
                    .build();
            queueService.put(metricsQueueItem);
        } catch (IOException e) {
            log.error("Failed to convert resourceMetrics:{} to json", resourceMetrics, e);
            throw new ArktourosException(e, "failed to convert resourceMetrics to json");
        }
    }

    public static void setQueueService(MetricsQueueService queueService) {
        OtelMetricsAnalyzer.queueService = queueService;
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            transform();
        }
    }

    public void transform() {
        MetricsQueueItem item = queueService.get();
        if (item != null && StringUtils.isNotEmpty(item.getData())) {
            log.info("OtelMetricsAnalyzer start to transform data");
        } else {
            log.warn("OtelMetricsAnalyzer get null data from queue, continue for next.");
            return;
        }
        try {
            ResourceMetrics.Builder resourceMetricsBuilder = ResourceMetrics.newBuilder();
            ProtoBufJsonUtils.fromJSON(item.getData(), resourceMetricsBuilder);
            ResourceMetrics resourceMetrics = resourceMetricsBuilder.build();
            io.opentelemetry.proto.resource.v1.Resource resource = resourceMetrics.getResource();
            Map<String, String> attributes = OtelAnalyzerUtil
                    .convertAttributesToMap(resource.getAttributesList());
            log.debug("Now OtelMetricsAnalyzer is going to change otel data to arktouros data");
            List<ScopeMetrics> scopeMetricsList = resourceMetrics.getScopeMetricsList();
            for (ScopeMetrics scopeMetrics : scopeMetricsList) {
                scopeMetrics.getMetricsList().stream()
                        .flatMap(metric ->
                                adaptMetrics(attributes, metric))
                        .forEach(metric -> {
                            try {
                                sinkService.sink(metric);
                            } catch (IOException e) {
                                log.error("Failed to sink metric after retry:{}", metric, e);
                                throw new ArktourosException(e, "failed to sink metric");
                            }
                        });
            }
        } catch (IOException e) {
            log.error("OtelMetricsAnalyzer failed to serialize data:{}", item.getData(), e);
            throw new ArktourosException(e, "failed to serialize data");
        }
    }

    @Override
    public void interrupt() {
        log.info("OtelMetricsAnalyzer is shutting down.");
        if (!queueService.isEmpty()) {
            log.warn("Otel metric data has remained in cache. Some data will be lost.");
        }
        queueService.clear();
        super.interrupt();
    }

    // Adapt the OpenTelemetry metrics to Prometheus metrics
    private Stream<? extends Metric> adaptMetrics(final Map<String, String> nodeLabels, final io.opentelemetry.proto.metrics.v1.Metric metric) {
        if (metric.hasGauge()) {
            return processGaugeMetric(nodeLabels, metric);
        }
        if (metric.hasSum()) {
            return processSumMetric(nodeLabels, metric);
        }
        if (metric.hasHistogram()) {
            return processHistogramMetric(nodeLabels, metric);
        }
        if (metric.hasExponentialHistogram()) {
            return processExponentialHistogram(nodeLabels, metric);
        }
        if (metric.hasSummary()) {
            return processSummaryMetric(nodeLabels, metric);
        }
        throw new UnsupportedOperationException("Unsupported type");
    }

    private static Stream<Summary> processSummaryMetric(Map<String, String> nodeLabels, io.opentelemetry.proto.metrics.v1.Metric metric) {
        return metric.getSummary()
                .getDataPointsList()
                .stream()
                .map(point -> Summary.builder()
                        .name(metric.getName())
                        .description(metric.getDescription())
                        .labels(mergeLabels(nodeLabels, buildLabels(point.getAttributesList())))
                        .sampleCount(point.getCount())
                        .sampleSum(point.getSum())
                        .quantiles(point.getQuantileValuesList()
                                .stream()
                                .collect(toMap(SummaryDataPoint.ValueAtQuantile::getQuantile,
                                        SummaryDataPoint.ValueAtQuantile::getValue)))
                        .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                        .build()
                );
    }

    private static Stream<Histogram> processExponentialHistogram(Map<String, String> nodeLabels, io.opentelemetry.proto.metrics.v1.Metric metric) {
        return metric.getExponentialHistogram().getDataPointsList().stream()
                // exponential histogram也转成histogram
                .map(point -> Histogram
                        .builder()
                        .name(metric.getName())
                        .description(metric.getDescription())
                        .labels(mergeLabels(nodeLabels, buildLabels(point.getAttributesList())))
                        .sampleCount(point.getCount())
                        .sampleSum(point.getSum())
                        .buckets(buildBucketsFromExponentialHistogram(point.getPositive().getOffset(),
                                point.getPositive().getBucketCountsList(),
                                point.getNegative().getOffset(),
                                point.getNegative().getBucketCountsList(),
                                point.getScale()))
                        .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                        .build()
                );
    }

    private static Stream<Histogram> processHistogramMetric(Map<String, String> nodeLabels, io.opentelemetry.proto.metrics.v1.Metric metric) {
        return metric.getHistogram()
                .getDataPointsList()
                .stream()
                .map(point -> Histogram
                        .builder()
                        .name(metric.getName())
                        .description(metric.getDescription())
                        .labels(mergeLabels(nodeLabels, buildLabels(point.getAttributesList())))
                        .sampleCount(point.getCount())
                        .sampleSum(point.getSum())
                        .buckets(buildBuckets(point.getBucketCountsList(),
                                point.getExplicitBoundsList()))
                        .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                        .build()
                );
    }

    private Stream<? extends Metric> processSumMetric(Map<String, String> nodeLabels, io.opentelemetry.proto.metrics.v1.Metric metric) {
        final Sum sum = metric.getSum();
        if (sum.getAggregationTemporality() == AGGREGATION_TEMPORALITY_UNSPECIFIED) {
            return Stream.empty();
        }
        if (sum.getAggregationTemporality() == AGGREGATION_TEMPORALITY_DELTA) {
            return sum.getDataPointsList().stream().map(point -> Gauge
                    .builder()
                    .name(metric.getName())
                    .labels(mergeLabels(nodeLabels,
                            buildLabels(point.getAttributesList())))
                    .description(metric.getDescription())
                    .value(point.hasAsDouble() ? point.getAsDouble() : point.getAsInt())
                    .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                    .build());
        }
        if (sum.getIsMonotonic()) {
            return sum.getDataPointsList()
                    .stream()
                    .map(point -> Counter
                            .builder()
                            .name(metric.getName())
                            .description(metric.getDescription())
                            .labels(mergeLabels(nodeLabels, buildLabels(point.getAttributesList())))
                            .value(point.hasAsDouble() ? point.getAsDouble() : point.getAsInt())
                            .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                            .build()
                    );
        } else {
            return sum.getDataPointsList()
                    .stream()
                    .map(point -> Gauge.builder()
                            .name(metric.getName())
                            .labels(mergeLabels(nodeLabels,
                                    buildLabels(point.getAttributesList())))
                            .description(metric.getDescription())
                            .value(point.hasAsDouble() ? point.getAsDouble() : point.getAsInt())
                            .timestamp(TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                            .build());
        }
    }

    private Stream<Gauge> processGaugeMetric(
            Map<String, String> nodeLabels,
            io.opentelemetry.proto.metrics.v1.Metric metric) {
        return metric.getGauge()
                .getDataPointsList()
                .stream()
                .map(point -> Gauge
                        .builder()
                        .name(metric.getName())
                        .labels(mergeLabels(nodeLabels,
                                buildLabels(point.getAttributesList())))
                        .description(metric.getDescription())
                        .value(point.hasAsDouble() ?
                                point.getAsDouble() : point.getAsInt())
                        .timestamp(
                                TimeUnit.NANOSECONDS.toMillis(point.getTimeUnixNano()))
                        .build());
    }

    private static Map<String, String> buildLabels(List<KeyValue> kvs) {
        return kvs.stream().collect(toMap(
                KeyValue::getKey,
                it -> it.getValue().getStringValue()));
    }

    private static Map<String, String> mergeLabels(final Map<String, String> nodeLabels, final Map<String, String> pointLabels) {

        // data point labels should have higher precedence and override the one in node labels
        final Map<String, String> result = new HashMap<>(nodeLabels);
        result.putAll(pointLabels);
        return result;
    }

    private static Map<Double, Long> buildBuckets(final List<Long> bucketCounts, final List<Double> explicitBounds) {

        final Map<Double, Long> result = new HashMap<>();
        for (int i = 0; i < explicitBounds.size(); i++) {
            result.put(explicitBounds.get(i), bucketCounts.get(i));
        }
        result.put(Double.POSITIVE_INFINITY, bucketCounts.get(explicitBounds.size()));
        return result;
    }

    /**
     * ExponentialHistogram data points are an alternate representation to the Histogram data point in OpenTelemetry
     * metric format(<a href="https://opentelemetry.io/docs/reference/specification/metrics/data-model/#exponentialhistogram">...</a>).
     * It uses scale, offset and bucket index to calculate the bound. Firstly, calculate the base using scale by
     * formula: base = 2**(2**(-scale)). Then the upperBound of specific bucket can be calculated by formula:
     * base**(offset+index+1). Above calculation way is about positive buckets. For the negative case, we just
     * map them by their absolute value into the negative range using the same scale as the positive range. So the
     * upperBound should be calculated as -base**(offset+index).
     * <p>
     * Ignored the zero_count field temporarily,
     * because the zero_threshold even could overlap the existing bucket scopes.
     *
     * @param positiveOffset       corresponding to positive Buckets' offset in ExponentialHistogramDataPoint
     * @param positiveBucketCounts corresponding to positive Buckets' bucket_counts in ExponentialHistogramDataPoint
     * @param negativeOffset       corresponding to negative Buckets' offset in ExponentialHistogramDataPoint
     * @param negativeBucketCounts corresponding to negative Buckets' bucket_counts in ExponentialHistogramDataPoint
     * @param scale                corresponding to scale in ExponentialHistogramDataPoint
     * @return The map is a bucket set for histogram, the key is specific bucket's upperBound, the value is item count
     * in this bucket lower than or equals to key(upperBound)
     */
    private static Map<Double, Long> buildBucketsFromExponentialHistogram(int positiveOffset, final List<Long> positiveBucketCounts, int negativeOffset, final List<Long> negativeBucketCounts, int scale) {

        final Map<Double, Long> result = new HashMap<>();
        double base = Math.pow(2.0, Math.pow(2.0, -scale));
        if (base == Double.POSITIVE_INFINITY) {
            log.warn("Receive and reject out-of-range ExponentialHistogram data");
            return result;
        }
        double upperBound;
        for (int i = 0; i < negativeBucketCounts.size(); i++) {
            upperBound = -Math.pow(base, negativeOffset + i);
            if (upperBound == Double.NEGATIVE_INFINITY) {
                log.warn("Receive and reject out-of-range ExponentialHistogram data");
                return new HashMap<>();
            }
            result.put(upperBound, negativeBucketCounts.get(i));
        }
        for (int i = 0; i < positiveBucketCounts.size() - 1; i++) {
            upperBound = Math.pow(base, positiveOffset + i + 1);
            if (upperBound == Double.POSITIVE_INFINITY) {
                log.warn("Receive and reject out-of-range ExponentialHistogram data");
                return new HashMap<>();
            }
            result.put(upperBound, positiveBucketCounts.get(i));
        }
        result.put(Double.POSITIVE_INFINITY, positiveBucketCounts.getLast());
        return result;
    }

    @Override
    public void init() {
        log.info("Initializing OtelMetricsAnalyzer:{}, creating table APM_METRICS_QUEUE",
                this.getName());
        queueService.waitTableReady();
        log.info("OtelTraceAnalyzer is ready.");
    }
}

package edu.npu.arktouros.service.scheduled.sqlite;

import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.topology.span.SpanTreeNode;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.service.search.SearchService;
import edu.npu.arktouros.service.sinker.SinkService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SqliteScheduledJob extends ScheduledJob {
    private static final long WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(5);

    public SqliteScheduledJob(SearchService searchService, SinkService sinkService) {
        super(searchService, sinkService);
    }

    @Override
    public void start() {
        calculateThroughputThreadPool.scheduleAtFixedRate(() -> runSafely(this::calculateThroughput), 5, 5, TimeUnit.MINUTES);
        calculateResponseTimeThreadPool.scheduleAtFixedRate(() -> runSafely(this::calculateResponseTime), 5, 5, TimeUnit.MINUTES);
        calculateErrorRateThreadPool.scheduleAtFixedRate(() -> runSafely(this::calculateErrorRate), 5, 5, TimeUnit.MINUTES);
    }

    private void runSafely(Runnable job) {
        try {
            job.run();
        } catch (RuntimeException e) {
            log.error("SQLite metric calculation failed; the next interval will retry", e);
        }
    }

    @Override
    public void stop() {
        rolloverThreadPool.shutdown();
        calculateThroughputThreadPool.shutdown();
        calculateResponseTimeThreadPool.shutdown();
        calculateErrorRateThreadPool.shutdown();
        simulateMetricThreadPool.shutdown();
    }

    @Override
    protected void rollover() {
        // SQLite has no Elasticsearch index aliases to roll over.
    }

    @Override
    protected void simulateMetrics() {
        // Production storage must not add synthetic measurements.
    }

    @Override
    protected void calculateThroughput() {
        long end = System.currentTimeMillis();
        for (Service service : searchService.getAllServices()) {
            writeGauge(service, "throughput", searchService.getTraceCount(service, end - WINDOW_MILLIS, end) / 5.0, end);
        }
    }

    @Override
    protected void calculateResponseTime() {
        long end = System.currentTimeMillis();
        for (Service service : searchService.getAllServices()) {
            List<String> traceIds = searchService.getEndPointTraceIdVos(
                            new EndPointQueryDto(service.getName(), 1, Integer.MAX_VALUE, end - WINDOW_MILLIS, end))
                    .stream().flatMap(endpoint -> endpoint.traceIds().stream()).distinct().toList();
            List<Long> times = new ArrayList<>();
            for (String traceId : traceIds) {
                searchService.getSpanTreeInMinutes(service.getName(), traceId, end - WINDOW_MILLIS, end)
                        .stream().map(this::costTime).forEach(times::add);
            }
            double average = times.stream().mapToLong(Long::longValue).average().orElse(0);
            writeGauge(service, "response_time", average, end);
        }
    }

    private long costTime(SpanTreeNode node) {
        Span span = node.getSpan();
        if (span.getEndTime() == PersistentDataConstants.ERROR_SPAN_END_TIME) return 0;
        return span.getEndTime() - span.getStartTime()
                + node.getChildren().stream().mapToLong(this::costTime).max().orElse(0);
    }

    @Override
    protected void calculateErrorRate() {
        long end = System.currentTimeMillis();
        for (Service service : searchService.getAllServices()) {
            List<Span> spans = searchService.getAllSpans(service, end - WINDOW_MILLIS).stream()
                    .filter(span -> span.getStartTime() <= end).toList();
            List<Span> errors = spans.stream()
                    .filter(span -> span.getEndTime() == PersistentDataConstants.ERROR_SPAN_END_TIME).toList();
            writeGauge(service, "error_rate", spans.isEmpty() ? 0 : (double) errors.size() / spans.size(), end);
        }
    }

    private void writeGauge(Service service, String name, double value, long timestamp) {
        Gauge gauge = Gauge.builder().name(name).description("Calculated over the previous 5 minutes.")
                .labels(Map.of()).value(value).timestamp(timestamp).build();
        gauge.setServiceName(service.getName());
        try {
            sinkService.sink(gauge);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot persist " + name + " for " + service.getName(), e);
        }
    }
}

package edu.npu.arktouros.sqlite;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.npu.arktouros.config.SqliteStorageConfig;
import edu.npu.arktouros.mapper.search.sqlite.SqliteSearchMapper;
import edu.npu.arktouros.model.common.PersistentDataConstants;
import edu.npu.arktouros.model.dto.*;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.*;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.*;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.file.json.JsonFilePreHandler;
import edu.npu.arktouros.service.operation.sqlite.SqliteOperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.service.scheduled.sqlite.SqliteScheduledJob;
import edu.npu.arktouros.service.search.SearchServiceImpl;
import edu.npu.arktouros.service.sinker.sqlite.SqliteSinkService;
import edu.npu.arktouros.util.sqlite.SqliteStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SqliteStorageTest {
    @TempDir Path directory;
    private SqliteStore store;
    private SqliteSinkService sink;
    private SqliteSearchMapper mapper;

    @BeforeEach
    void open() throws IOException {
        store = new SqliteStore(directory.resolve("nested/telemetry.sqlite"));
        sink = new SqliteSinkService(store);
        sink.init();
        mapper = new SqliteSearchMapper(store);
    }

    @AfterEach
    void close() {
        store.close();
    }

    private Span span(String id, String service, String trace, long start, long end) {
        return Span.builder().id(id).serviceName(service).name("work").traceId(trace)
                .parentSpanId("").root(true).startTime(start).endTime(end).build();
    }

    private Log log(String service, String text, long timestamp) {
        return Log.builder().serviceName(service).content(text).timestamp(timestamp)
                .traceId("trace").severityText("INFO").build();
    }

    private PageResultVo<?> logs(LogQueryDto dto) {
        return (PageResultVo<?>) mapper.getLogListByQuery(dto).get("result");
    }

    @Test
    void survivesReopenAndUpsertsSpansWithoutOverwritingServiceMetadata() throws Exception {
        Service service = Service.builder().name("a").status(true).build();
        service.setLatency(42);
        service.setNodeName("node");
        sink.sink(service);
        sink.sink(span("s1", "a", "t1", 100, 110));
        sink.sink(span("s1", "a", "t1", 100, 120));
        sink.sink(log("a", "hello", 105));
        store.close();
        open();
        assertTrue(sink.isReady());
        assertEquals("node", mapper.getServiceByName("a").getNodeName());
        assertEquals(42, mapper.getServiceByName("a").getLatency());
        List<Span> spans = mapper.getSpanListByServiceNames(List.of("a"));
        assertEquals(1, spans.size());
        assertEquals(120L, spans.getFirst().getEndTime());
        assertEquals(1, mapper.getAllServices().size());
        PageResultVo<?> page = (PageResultVo<?>) mapper.getServiceList(new ServiceQueryDto(1, 10, "a", null)).get("result");
        assertEquals(20, ((Service) page.data().getFirst()).getLatency());
        assertEquals(1, mapper.getServiceListFromTopologyQuery("default").size());
        assertEquals(List.of("default"), mapper.getNamespaceList("def").get("result"));
    }

    @Test
    void filtersLogsInSqlWithStablePaginationAndLiteralParameters() throws Exception {
        sink.sink(log("a", "hello world 100%_中文", 100));
        sink.sink(log("a", "world hello skip", 200));
        sink.sink(log(null, "hello", 300));
        assertEquals(3L, logs(new LogQueryDto(1, 1, null, null, null, null, null, null, null)).total());
        PageResultVo<?> page = logs(new LogQueryDto(2, 1, null, null, null, null, null, null, null));
        assertEquals(200L, ((Log) page.data().getFirst()).getTimestamp());
        assertEquals(1L, logs(new LogQueryDto(1, 10, "a", "trace", "HELLO world", "skip", "INFO", 100L, 200L)).total());
        assertEquals(1L, logs(new LogQueryDto(1, 10, "a", null, "%_中文", null, null, null, null)).total());
        assertEquals(0L, logs(new LogQueryDto(1, 10, "' OR 1=1 --", null, null, null, null, null, null)).total());
        assertEquals(1L, logs(new LogQueryDto(1, 10, "null", null, null, null, null, null, null)).total());
        assertEquals(List.of("INFO"), mapper.getAllLogLevels("IN").get("result"));
        assertThrows(IllegalArgumentException.class,
                () -> mapper.getLogListByQuery(new LogQueryDto(0, 10, null, null, null, null, null, null, null)));
    }

    @Test
    void preservesBothSpanTimeBoundsAndTraceScope() throws Exception {
        sink.sink(span("early", "a", "t", 90, 110));
        sink.sink(span("inside", "a", "t", 100, 150));
        sink.sink(span("late", "a", "t", 120, 210));
        sink.sink(span("other", "b", "t", 110, 160));
        sink.sink(span("outside", "a", "other", 400, 500));
        assertEquals(List.of("inside"), mapper.getSpanListBySpanNameAndServiceName(
                new SpanTimesQueryDto("work", "a", 100L, 200L)).stream().map(Span::getId).toList());
        assertEquals(1, mapper.getSpanListByTraceId(new SpanTopologyQueryDto("t", "a", true, 100L, 200L)).size());
        assertEquals(2, mapper.getSpanListByTraceId(new SpanTopologyQueryDto("t", "a", false, 100L, 200L)).size());
        assertEquals(1, mapper.getTraceCount(mapper.getServiceByName("a"), 100, 200));
        assertEquals(1, mapper.getSpanListByTraceId("a", "t", 100, 200).size());
        assertEquals(3, mapper.getAllSpans(mapper.getServiceByName("a"), 100).size());
        assertEquals(List.of("work"), mapper.getSpanNamesByServiceName(new SpanNamesQueryDto("a", 100L, 200L)).get("result"));
        assertEquals(1, mapper.getSpanTimesVoBySpanName(new SpanTimesQueryDto("work", "a", 100L, 200L)).getXAxis().size());
        assertEquals(new NamespaceTopoTimeRangeVo(90L, 500L), mapper.getTimeRange().get("result"));
        assertTrue(mapper.getSpanListByServiceNames(List.of()).isEmpty());
    }

    @Test
    void keepsEndpointGroupsAndBuildsTopologyThroughExistingSearchService() throws Exception {
        EndPoint endpoint = new EndPoint();
        endpoint.setServiceName("a");
        endpoint.setIp("127.0.0.1");
        Span root = span("root", "a", "t", 100, 200);
        root.setLocalEndPoint(endpoint);
        Span child = span("child", "b", "t", 110, 190);
        child.setParentSpanId("root");
        child.setRoot(false);
        child.setName("child-work");
        sink.sink(root);
        sink.sink(child);
        List<EndPointTraceIdVo> endpoints = mapper.getEndPointTraceIdVos(new EndPointQueryDto("a", 1, 10, null, null));
        assertEquals(endpoint, endpoints.getFirst().endPoint());
        assertEquals(Set.of("t"), endpoints.getFirst().traceIds());
        R page = mapper.getEndPointListByServiceName(new EndPointQueryDto("a", 2, 1, null, null));
        assertEquals(1, page.get("total"));
        assertEquals(List.of(), page.get("result"));
        SearchServiceImpl service = new SearchServiceImpl();
        ReflectionTestUtils.setField(service, "searchMapper", mapper);
        assertNotNull(service.getServiceTopology("default", 300L).get("result"));
        assertNotNull(service.getSpanTopologyByTraceId(new SpanTopologyQueryDto("t", "a", false, null, null)).get("result"));
    }

    @Test
    void roundTripsAllMetricTypesAndSamplesEachNameAndType() throws Exception {
        List<Metric> metrics = List.of(
                Gauge.builder().name("g").labels(Map.of()).timestamp(100).value(1.25).build(),
                Counter.builder().name("c").labels(Map.of()).timestamp(100).value(2).build(),
                Histogram.builder().name("h").labels(Map.of()).timestamp(100).sampleCount(2)
                        .sampleSum(3).buckets(Map.of(5.0, 2L)).build(),
                Summary.builder().name("s").labels(Map.of()).timestamp(100).sampleCount(2)
                        .sampleSum(3).quantiles(Map.of(0.5, 1.5)).build());
        for (Metric metric : metrics) {
            metric.setServiceName("a");
            sink.sink(metric);
        }
        List<String> names = mapper.getMetricsNames("a", null);
        assertEquals(List.of("c", "g", "h", "s"), names);
        assertEquals(2, mapper.getMetricsNames("a", 2).size());
        assertEquals(4, mapper.getMetricsValues(names, "a", 100L, 100L).size());
        assertEquals(1.25, mapper.getMetricsValues(List.of("g"), "a", null, null).getFirst().value());
        Gauge later = Gauge.builder().name("g").labels(Map.of()).timestamp(200).value(9).build();
        later.setServiceName("a");
        sink.sink(later);
        later.setTimestamp(3600100);
        sink.sink(later);
        assertEquals(3, mapper.getMetricsValues(List.of("g"), "a", null, null).size());
        List<Metric> sampled = mapper.getMetricsValues(List.of("g"), "a", 0L, 7200000L);
        assertEquals(2, sampled.size());
        assertEquals(100, sampled.getFirst().getTimestamp());
        assertTrue(mapper.getMetricsValues(List.of(), "a", null, null).isEmpty());
    }

    @Test
    void deletionIsSelectiveAndDoesNotRestartOrBlockTheReceiver() throws Exception {
        DataReceiver receiver = mock(DataReceiver.class);
        ScheduledJob jobs = mock(ScheduledJob.class);
        SqliteOperationService operations = new SqliteOperationService(receiver, jobs, store);
        sink.sink(log("a", "hello", 100));
        sink.sink(span("s", "a", "t", 100, 200));
        Gauge gauge = Gauge.builder().name("g").labels(Map.of()).timestamp(100).build();
        gauge.setServiceName("a");
        sink.sink(gauge);
        operations.deleteAllLogs();
        assertEquals(0L, logs(new LogQueryDto(1, 10, null, null, null, null, null, null, null)).total());
        assertEquals(1, mapper.getSpanListByServiceNames(List.of("a")).size());
        operations.deleteAllMetrics();
        assertTrue(mapper.getMetricsNames("a", null).isEmpty());
        operations.deleteAllSpans();
        assertEquals(new NamespaceTopoTimeRangeVo(null, null), mapper.getTimeRange().get("result"));
        assertEquals(1, mapper.getAllServices().size());
        operations.deleteAllData();
        assertTrue(mapper.getAllServices().isEmpty());
        sink.sink(log("a", "after clear", 300));
        assertEquals(1, mapper.getAllServices().size());
        verifyNoInteractions(receiver, jobs);
    }

    @Test
    void concurrentWritesPersistEveryEvent() throws Exception {
        try (ExecutorService workers = Executors.newFixedThreadPool(8)) {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                int id = i;
                results.add(workers.submit(() -> {
                    sink.sink(log("a", "event-" + id, id));
                    return null;
                }));
            }
            for (Future<?> future : results) future.get(20, TimeUnit.SECONDS);
        }
        assertEquals(80L, logs(new LogQueryDto(1, 10, null, null, null, null, null, null, null)).total());
        assertEquals(1, mapper.getAllServices().size());
    }

    @Test
    void failedWriteRollsBackServiceCreation() {
        store.jdbc().getJdbcTemplate().execute("""
                CREATE TRIGGER reject_logs BEFORE INSERT ON telemetry WHEN NEW.kind='log'
                BEGIN SELECT RAISE(ABORT,'test failure'); END
                """);
        assertThrows(IOException.class, () -> sink.sink(log("a", "rejected", 100)));
        assertTrue(mapper.getAllServices().isEmpty());
        assertThrows(IOException.class, () -> sink.sink(span("", "a", "t", 100, 200)));
    }

    @Test
    void scheduledCalculationsUseSqliteWithoutElasticsearch() throws Exception {
        long now = System.currentTimeMillis();
        sink.sink(span("ok", "a", "t", now - 100, now - 50));
        sink.sink(span("err", "a", "error", now - 100, PersistentDataConstants.ERROR_SPAN_END_TIME));
        assertFalse(mapper.getServiceByName("a").isStatus());
        assertDoesNotThrow(() -> mapper.getServiceList(new ServiceQueryDto(1, 10, "a", null)));
        SearchServiceImpl service = new SearchServiceImpl();
        ReflectionTestUtils.setField(service, "searchMapper", mapper);
        SqliteScheduledJob job = new SqliteScheduledJob(service, sink);
        try {
            ReflectionTestUtils.invokeMethod(job, "calculateThroughput");
            ReflectionTestUtils.invokeMethod(job, "calculateResponseTime");
            ReflectionTestUtils.invokeMethod(job, "calculateErrorRate");
            List<Metric> metrics = mapper.getMetricsValues(mapper.getMetricsNames("a", null), "a", null, null);
            assertEquals(3, metrics.size());
            assertEquals(0.4, metrics.stream().filter(m -> m.getName().equals("throughput")).findFirst().orElseThrow().value());
            assertEquals(0.5, metrics.stream().filter(m -> m.getName().equals("error_rate")).findFirst().orElseThrow().value());
            assertFalse(mapper.getServiceByName("a").isStatus());
        } finally {
            job.stop();
        }
    }

    @Test
    void offlineJsonParserPersistsConcatenatedAndSplitRecords() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Gauge gauge = Gauge.builder().name("offline").labels(Map.of()).timestamp(110).value(3.5).build();
        gauge.setServiceName("a");
        String input = json.writeValueAsString(log("a", "离线 JSON", 100))
                + json.writeValueAsString(span("offline", "a", "trace", 100, 120))
                + json.writeValueAsString(gauge);
        ArrayBlockingQueue<String> chunks = new ArrayBlockingQueue<>(2);
        JsonFilePreHandler parser = new JsonFilePreHandler(chunks, "arktouros", null, sink, json, 1);
        chunks.add(input.substring(0, input.length() / 2));
        parser.handle();
        chunks.add(input.substring(input.length() / 2));
        parser.handle();
        assertEquals(1L, logs(new LogQueryDto(1, 10, null, null, null, null, null, null, null)).total());
        assertEquals(1, mapper.getSpanListByServiceNames(List.of("a")).size());
        assertEquals(3.5, mapper.getMetricsValues(List.of("offline"), "a", null, null).getFirst().value());
    }

    @Test
    void rejectsMixedStorageConfiguration() {
        MockEnvironment environment = new MockEnvironment().withProperty("instance.active.sinker", "sqlite")
                .withProperty("instance.active.searchMapper", "elasticsearch");
        assertThrows(IllegalArgumentException.class, () -> new SqliteStorageConfig().sqliteStore(environment));
    }
}

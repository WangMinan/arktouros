package edu.npu.arktouros;

import com.fasterxml.jackson.databind.JsonNode;
import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.mapper.search.sqlite.SqliteSearchMapper;
import edu.npu.arktouros.proto.collector.v1.*;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.operation.sqlite.SqliteOperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.service.scheduled.sqlite.SqliteScheduledJob;
import edu.npu.arktouros.service.sinker.SinkService;
import edu.npu.arktouros.service.sinker.sqlite.SqliteSinkService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "instance.active.sinker=sqlite", "instance.active.searchMapper=sqlite",
        "instance.active.operation=sqlite", "instance.active.scheduledJob=sqlite",
        "instance.active.dataReceiver=arktourosGrpc", "receiver.grpc.port=0",
        "spring.datasource.url=jdbc:h2:mem:sqlite-api-queues;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
@DirtiesContext
class SqliteApiTest {
    @TempDir static Path directory;
    @Resource TestRestTemplate http;
    @Resource DataReceiver receiver;
    @Resource SearchMapper mapper;
    @Resource SinkService sink;
    @Resource ScheduledJob scheduled;
    @Resource DataOperationService operations;
    @Resource DataSource dataSource;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("sqlite.path", () -> directory.resolve("api.sqlite").toString());
    }

    @AfterEach
    void stop() {
        receiver.stop();
        scheduled.stop();
    }

    @Test
    void receivesGrpcAndServesEveryQueryAndDeleteEndpointWithoutElasticsearch() throws Exception {
        assertInstanceOf(SqliteSearchMapper.class, mapper);
        assertInstanceOf(SqliteSinkService.class, sink);
        assertInstanceOf(SqliteScheduledJob.class, scheduled);
        assertInstanceOf(SqliteOperationService.class, operations);
        try (var connection = dataSource.getConnection()) {
            assertEquals("H2", connection.getMetaData().getDatabaseProductName());
        }
        Server server = (Server) ReflectionTestUtils.getField(receiver, "server");
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
        try {
            var log = edu.npu.arktouros.proto.log.v1.Log.newBuilder().setServiceName("api-test")
                    .setTraceId("trace").setSpanId("span").setContent("SQLite round trip 中文")
                    .setSeverityText("INFO").setTimestamp(1000).build();
            assertEquals(0, LogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
                    .export(LogRequest.newBuilder().addLogs(log).build()).getRejectedLogRecords());
            var span = edu.npu.arktouros.proto.span.v1.Span.newBuilder().setId("span").setServiceName("api-test")
                    .setName("work").setTraceId("trace").setRoot(true).setStartTime(1000).setEndTime(1050)
                    .addTags(edu.npu.arktouros.proto.common.v1.Tag.newBuilder().setKey("key").setValue("value")).build();
            assertEquals(0, SpanServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
                    .export(SpanRequest.newBuilder().addSpans(span).build()).getRejectedSpanRecords());
            var metric = edu.npu.arktouros.proto.metric.v1.Metric.newBuilder().setServiceName("api-test")
                    .setName("cpu").setTimestamp(1000).build();
            var gauge = edu.npu.arktouros.proto.metric.v1.Gauge.newBuilder().setMetric(metric).setValue(12.5).build();
            assertEquals(0, MetricServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
                    .export(MetricRequest.newBuilder().addMetrics(Metric.newBuilder().setGauge(gauge)).build())
                    .getRejectedMetricRecords());

            assertEquals("SQLite round trip 中文", get("/logs?pageNum=1&pageSize=10").path("result").path("data").get(0).path("content").asText());
            assertEquals(1, get("/services?pageNum=1&pageSize=10").path("result").path("total").asInt());
            assertEquals("INFO", get("/log/levels").path("result").get(0).asText());
            assertEquals("default", get("/service/namespaces").path("result").get(0).asText());
            assertEquals(1000, get("/service/time-range").path("result").path("startTimestamp").asLong());
            for (String endpoint : new String[]{"/service/topology", "/trace/endPoints?serviceName=api-test&pageNum=1&pageSize=10",
                    "/trace/topology?serviceName=api-test&traceId=trace&innerService=true",
                    "/trace/spanNames?serviceName=api-test", "/trace/spanTimes?serviceName=api-test&spanName=work",
                    "/metrics?serviceName=api-test"}) {
                assertTrue(get(endpoint).has("result"), endpoint);
            }
            http.delete("/logs");
            assertEquals(0, get("/logs?pageNum=1&pageSize=10").path("result").path("total").asInt());
            http.delete("/metrics");
            assertFalse(get("/metrics?serviceName=api-test").has("result"));
            http.delete("/trace/spans");
            assertTrue(get("/trace/spanNames?serviceName=api-test").path("result").isEmpty());
            http.delete("/all");
            assertEquals(0, get("/services?pageNum=1&pageSize=10").path("result").path("total").asInt());
            assertEquals(0, LogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS)
                    .export(LogRequest.newBuilder().addLogs(log).build()).getRejectedLogRecords());
            assertEquals(1, get("/logs?pageNum=1&pageSize=10").path("result").path("total").asInt());
        } finally {
            channel.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private JsonNode get(String path) {
        var response = http.getForEntity(path, JsonNode.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), path);
        assertNotNull(response.getBody());
        assertEquals(2000, response.getBody().path("code").asInt(), path + ": " + response.getBody());
        return response.getBody();
    }
}

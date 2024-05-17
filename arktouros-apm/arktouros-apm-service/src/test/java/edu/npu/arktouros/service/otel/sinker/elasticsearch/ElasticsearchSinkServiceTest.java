package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : {@link ElasticsearchSinkService}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class ElasticsearchSinkServiceTest {

    @Resource
    private SinkService sinkService;

    private ElasticsearchSinkService elasticsearchSinkService;

    private ElasticsearchClient esClient;
    private static MockedStatic<ElasticsearchClientPool> poolMockedStatic;
    private static MockedStatic<ElasticsearchUtil> elasticsearchUtil;

    @BeforeAll
    static void init() {
        PropertiesProvider.init();
        poolMockedStatic = Mockito.mockStatic(ElasticsearchClientPool.class);
        elasticsearchUtil = Mockito.mockStatic(ElasticsearchUtil.class);
    }

    @AfterAll
    static void close() {
        poolMockedStatic.close();
        elasticsearchUtil.close();
    }

    @BeforeEach
    void beforeEach() {
        esClient = Mockito.mock(ElasticsearchClient.class);
        poolMockedStatic.when(ElasticsearchClientPool::getClient).thenReturn(esClient);
        elasticsearchSinkService = new ElasticsearchSinkService();
    }

    // init方法不好测 跨线程了之后桩子插不进去 而且直接System.exit(1) 拿不到回调
    @Test
    @Disabled
    void testInit() {
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.init());
    }

    @Test
    void testCheckAndCreate() throws IOException {
        BooleanResponse exist = Mockito.mock(BooleanResponse.class);
        ElasticsearchIndicesClient indices = Mockito.mock(ElasticsearchIndicesClient.class);
        Mockito.when(esClient.indices()).thenReturn(indices);
        Mockito.when(indices.exists(any(Function.class)))
                .thenReturn(exist);
        Mockito.when(exist.value()).thenReturn(true);
        Assertions.assertDoesNotThrow(() ->
                elasticsearchSinkService
                        .checkAndCreate("test", new CountDownLatch(1)));
        Mockito.when(exist.value()).thenReturn(false);
        CreateIndexResponse response = Mockito.mock(CreateIndexResponse.class);
        Mockito.when(indices.create(any(CreateIndexRequest.class))).thenReturn(response);
        Mockito.when(response.acknowledged()).thenReturn(true);
        for (String index : ElasticsearchIndex.getIndexList()) {
            Assertions.assertDoesNotThrow(() ->
                    elasticsearchSinkService
                            .checkAndCreate(index, new CountDownLatch(1)));
        }
        Mockito.when(response.acknowledged()).thenReturn(false);
        Assertions.assertThrows(IOException.class, () -> elasticsearchSinkService
                .checkAndCreate("arktouros-log", new CountDownLatch(1)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> elasticsearchSinkService
                .checkAndCreate("arktouros-test", new CountDownLatch(1)));
    }

    @Test
    void testSink() throws IOException {
        // Mockito.doNothing().when(esClient.index(any(Function.class)));
        Counter counter = Counter.builder().name("counter").labels(new HashMap<>()).build();
        counter.setServiceName("test");
        Gauge gauge = Gauge.builder().name("gauge").labels(new HashMap<>()).build();
        gauge.setServiceName("test");
        Summary summary = Summary.builder().name("summary").labels(new HashMap<>()).build();
        summary.setServiceName("test");
        Histogram histogram = Histogram.builder().name("histogram").labels(new HashMap<>()).build();
        histogram.setServiceName("test");
        Log log1 = Log.builder().serviceName("test").build();
        Span span = Span.builder().serviceName("test").build();
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(counter));
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(gauge));
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(summary));
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(histogram));
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(log1));
        Assertions.assertDoesNotThrow(() -> elasticsearchSinkService.sink(span));
    }
}

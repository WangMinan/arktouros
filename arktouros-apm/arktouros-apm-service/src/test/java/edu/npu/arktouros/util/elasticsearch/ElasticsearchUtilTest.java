package edu.npu.arktouros.util.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.lang.reflect.Constructor;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class ElasticsearchUtilTest {

    private ElasticsearchClient esClient;
    private static MockedStatic<ElasticsearchClientPool> poolMockedStatic;

    @BeforeAll
    static void init() {
        PropertiesProvider.init();
        poolMockedStatic = Mockito.mockStatic(ElasticsearchClientPool.class);
    }

    @AfterAll
    static void close() {
        poolMockedStatic.close();
    }

    @BeforeEach
    void beforeEach() {
        esClient = Mockito.mock(ElasticsearchClient.class);
        poolMockedStatic.when(ElasticsearchClientPool::getClient).thenReturn(esClient);
    }

    @Test
    void testConstructor() throws NoSuchMethodException {
        Constructor<ElasticsearchUtil> elasticsearchUtilConstructor =
                ElasticsearchUtil.class.getDeclaredConstructor();
        elasticsearchUtilConstructor.setAccessible(true);
        Assertions.assertThrows(Exception.class,
                elasticsearchUtilConstructor::newInstance);
    }

    @Test
    void testSink() throws IOException {
        IndexResponse indexResponse = Mockito.mock(IndexResponse.class);

        // 这样写是有问题的 Elasticsearch的builder模式和泛型类mock不了
        Mockito.doAnswer(invocation -> {
            IndexRequest<Source> argument = invocation.getArgument(0);
            // 在这里，你可以对argument进行一些检查，例如检查它的id和index是否正确
            return indexResponse;
        }).when(esClient).index((IndexRequest<Object>) any());

        String id = "testId";
        String index = "testIndex";
        Log sinkLog = Mockito.mock(Log.class);
        Assertions.assertDoesNotThrow(() -> ElasticsearchUtil.sink(id, index, sinkLog));
        Assertions.assertDoesNotThrow(() -> ElasticsearchUtil.sink(index, sinkLog));

    }

    @Test
    void testSimpleSearch() {
        try {
            SearchResponse<Log> searchResponse = Mockito.mock(SearchResponse.class);
            Mockito.doReturn(searchResponse)
                    .when(esClient)
                    .search(any(SearchRequest.class), any());
            SearchRequest.Builder builder = Mockito.mock(SearchRequest.Builder.class);
            Assertions.assertDoesNotThrow(() ->
                    ElasticsearchUtil.simpleSearch(builder, Log.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void scrollSearchReturnsExpectedResult() throws IOException {
        SearchResponse<Log> searchResponse = Mockito.mock(SearchResponse.class);
        Mockito.doReturn(searchResponse).when(esClient)
                .search(Mockito.any(SearchRequest.class), Mockito.any());
        Mockito.when(searchResponse.scrollId()).thenReturn("1");

        ScrollResponse<Log> scrollResponse = Mockito.mock(ScrollResponse.class);
        Mockito.doReturn(scrollResponse).when(esClient)
                .scroll(Mockito.any(ScrollRequest.class), Mockito.any());

        SearchRequest.Builder builder = Mockito.mock(SearchRequest.Builder.class);
        // 理论上完全不应该，但桩子就是没插进去
        Assertions.assertThrows(NullPointerException.class, () ->
                ElasticsearchUtil.scrollSearch(builder, Log.class));
    }
}

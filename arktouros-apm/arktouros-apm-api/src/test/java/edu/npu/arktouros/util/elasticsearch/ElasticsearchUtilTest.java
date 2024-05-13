package edu.npu.arktouros.util.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
// 要加这个配置 不然对any的类型推断有很严格的限制
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class ElasticsearchUtilTest {

    @Mock
    private ElasticsearchClient esClient;

    @BeforeAll
    public static void init() {
        PropertiesProvider.init();
    }

    @Test
    public void testSink() throws IOException {
        try (
                MockedStatic<ElasticsearchClientPool> poolMockedStatic =
                        Mockito.mockStatic(ElasticsearchClientPool.class)
        ) {
            poolMockedStatic.when(ElasticsearchClientPool::getClient).thenReturn(esClient);
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
    }

    @Test
    void testSimpleSearch() {

    }
}

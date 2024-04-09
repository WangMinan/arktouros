package edu.npu.arktouros.mapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import edu.npu.arktouros.model.otel.structure.Service;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 测试ElasticSearchClient
 */
@SpringBootTest
@Slf4j
@Disabled // junit4 @Ignore = junit5 @Disabled
public class ElasticSearchClientTest {
    @Resource
    private ElasticsearchClient esClient;

    private static final String SERVICE_INDEX = "arktouros-service";
    private static final String LOG_INDEX = "arktouros-log";
    private static final String SPAN_INDEX = "arktouros-span";
    private static final String GAUGE_INDEX = "arktouros-gauge";
    private static final String COUNTER_INDEX = "arktouros-counter";
    private static final String SUMMARY_INDEX = "arktouros-summary";
    private static final String HISTOGRAM_INDEX = "arktouros-histogram";

    private static final List<String> indexList = List.of(SERVICE_INDEX, LOG_INDEX, SPAN_INDEX, GAUGE_INDEX,
            COUNTER_INDEX, SUMMARY_INDEX, HISTOGRAM_INDEX);

    @Test
    void testClientConnect() throws IOException {
        log.info("root path:{}", System.getProperty("user.dir"));
        BooleanResponse exists =
                esClient.indices().exists(builder -> builder.index("arktouros_log"));
        log.info("index exists: {}", exists.value());
    }

    @Test
    void testWriteSame() throws IOException {
        // 只会写一次
        Service service = Service.builder().name("test_service").build();
        esClient.index(
                builder -> builder
                        .index(SERVICE_INDEX)
                        .id(service.getId())
                        .document(service)
        );
        esClient.index(
                builder -> builder
                        .index(SERVICE_INDEX)
                        .id(service.getId())
                        .document(service)
        );
    }

    @Test
    void deleteMappings() {
        log.info("start deleting mappings");
        indexList.forEach(index -> {
            try {
                esClient.indices().delete(builder -> builder.index(index));
            } catch (IOException e) {
                log.error("delete index:{} failed", index);
            }
        });
    }
}

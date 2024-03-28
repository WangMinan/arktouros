package edu.npu.arktouros.mapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @author : [wangminan]
 * @description : 测试ElasticSearchClient
 */
@SpringBootTest
@Slf4j
public class ElasticSearchClientTest {
    @Resource
    private ElasticsearchClient esClient;

    @Test
    void testClient() throws IOException {
        log.info("root path:{}", System.getProperty("user.dir"));
        BooleanResponse exists =
                esClient.indices().exists(builder -> builder.index("arktouros_log"));
        log.info("index exists: {}", exists.value());
    }
}

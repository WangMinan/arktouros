package edu.npu.arktouros.service.otel.sinker.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.RolloverRequest;
import co.elastic.clients.elasticsearch.indices.RolloverResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ElasticsearchScheduledTasksTest {

    private ElasticsearchScheduledTasks elasticsearchScheduledTasks;
    private ElasticsearchClient esClient;

    @Test
    void testTryRollover() throws IOException {
        ElasticsearchIndicesClient indices = Mockito.mock(ElasticsearchIndicesClient.class);
        Mockito.when(esClient.indices()).thenReturn(indices);
        RolloverResponse response = Mockito.mock(RolloverResponse.class);
        Mockito.when(esClient.indices().rollover(any(RolloverRequest.class)))
                .thenReturn(response);
        Mockito.when(response.acknowledged()).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> elasticsearchScheduledTasks.tryRollover());
        Mockito.when(response.acknowledged()).thenReturn(false);
        Assertions.assertDoesNotThrow(() -> elasticsearchScheduledTasks.tryRollover());
    }
}

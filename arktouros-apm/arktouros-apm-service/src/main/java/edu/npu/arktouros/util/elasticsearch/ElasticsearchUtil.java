package edu.npu.arktouros.util.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticsearchClient的进一步封装
 */
@Slf4j
public class ElasticsearchUtil {

    public static void sink(String id, String index, Source source) throws IOException {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        esClient.index(builder -> builder.id(id).index(index).document(source));
        ElasticsearchClientPool.returnClient(esClient);
    }

    public static void sink(String index, Source source) throws IOException {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        esClient.index(builder -> builder.index(index).document(source));
        ElasticsearchClientPool.returnClient(esClient);
    }

    public static <T> SearchResponse<T> simpleSearch(
            SearchRequest.Builder searchRequestBuilder, Class<T> clazz
    ) {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        SearchResponse<T> searchResponse;
        try {
            searchResponse =
                    esClient.search(searchRequestBuilder.build(), clazz);
        } catch (IOException e) {
            log.error("Failed to search: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return searchResponse;
    }

    public static <T> List<T> scrollSearch(
            SearchRequest.Builder searchRequestBuilder, Class<T> clazz
    ) {
        List<T> result = new ArrayList<>();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        SearchResponse<T> searchResponse;
        try {
            searchResponse =
                    esClient.search(searchRequestBuilder
                            .scroll(new Time.Builder().time(
                                    PropertiesProvider
                                            .getProperty("elasticsearch.scroll.maxWait" + "ms", "5000ms")
                            ).build())
                            .build(), clazz);
            String scrollId = searchResponse.scrollId();
            result.addAll(searchResponse.hits().hits().stream().map(Hit::source).toList());
            ScrollResponse<T> scrollResponse;
            do {
                ScrollRequest scrollRequest = new ScrollRequest.Builder()
                        .scrollId(scrollId)
                        .scroll(new Time.Builder().time(
                                PropertiesProvider
                                        .getProperty("elasticsearch.scroll.maxWait" + "ms",
                                                "5000ms")
                        ).build())
                        .build();
                scrollResponse = esClient.scroll(scrollRequest, clazz);
                scrollId = scrollResponse.scrollId();
                result.addAll(scrollResponse.hits().hits().stream().map(Hit::source).toList());
            } while (!scrollResponse.hits().hits().isEmpty());
        } catch (IOException e) {
            log.error("Failed to scroll search: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return result;
    }
}

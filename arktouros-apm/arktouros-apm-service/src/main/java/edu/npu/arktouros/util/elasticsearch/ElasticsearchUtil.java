package edu.npu.arktouros.util.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.ScrollRequest;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.npu.arktouros.model.config.PropertiesProvider;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.model.otel.Source;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticsearchClient的进一步封装
 */
@Slf4j
public class ElasticsearchUtil {

    private ElasticsearchUtil() {
        throw new UnsupportedOperationException("ElasticsearchUtil is a utility class and cannot be instantiated");
    }

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
            throw new ArktourosException(e, "Failed to search");
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
                                            .getProperty(
                                                    "elasticsearch.scroll.maxWait",
                                                    "2000") + "ms"
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
                                        .getProperty(
                                                "elasticsearch.scroll.maxWait",
                                                "5000") + "ms"
                        ).build())
                        .build();
                scrollResponse = esClient.scroll(scrollRequest, clazz);
                scrollId = scrollResponse.scrollId();
                result.addAll(scrollResponse.hits().hits().stream().map(Hit::source).toList());
            } while (!scrollResponse.hits().hits().isEmpty());
        } catch (IOException e) {
            log.error("Failed to scroll search: {}", e.getMessage());
            throw new ArktourosException(e, "Failed to scroll search");
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return result;
    }

    public static <TDocument, TPartialDocument> boolean update(
            UpdateRequest<TDocument, TPartialDocument> updateRequest, Class<TDocument> clazz) {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        UpdateResponse<TDocument> updateResponse;
        try {
            updateResponse = esClient.update(updateRequest, clazz);
        } catch (IOException e) {
            log.error("Failed to update: {}", e.getMessage());
            throw new ArktourosException(e, "Failed to update");
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return updateResponse.result() != null;
    }

    public static boolean delete(DeleteRequest deleteRequest) {
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        DeleteResponse deleteResponse;
        try {
            deleteResponse = esClient.delete(deleteRequest);
        } catch (IOException e) {
            log.error("Failed to delete: {}", e.getMessage());
            throw new ArktourosException(e, "Failed to delete");
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return deleteResponse.result() != null;
    }

    /**
     * 获取所有实际索引 名称形式应当为虚拟索引+日期+序列号
     * @return 一个包含所有实际索引的列表
     */
    public static List<String> getAllArktourosIndexes() {
        List<String> indexes = new ArrayList<>();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        try {
            IndicesResponse indicesResponse = esClient.cat().indices();
            indicesResponse.valueBody().stream().filter(
                    indicesRecord -> {
                        if (StringUtils.isNotEmpty(indicesRecord.index())) {
                            return indicesRecord.index().startsWith("arktouros-");
                        }
                        return false;
                    }
            ).forEach(indicesRecord -> indexes.add(indicesRecord.index()));
            log.debug("All actual indexes: {}", indexes);
        } catch (IOException e) {
            log.error("Failed to get all actual indexes: {}", e.getMessage());
            throw new ArktourosException(e);
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
        return indexes;
    }

    /**
     * 删除指定索引
     * @param indexes 索引名称
     */
    public static void truncateIndexes(List<String> indexes) {
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest.Builder()
                .index(indexes)
                .query(new MatchAllQuery.Builder().build()._toQuery())
                .build();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        try {
            esClient.deleteByQuery(deleteByQueryRequest);
        } catch (IOException e) {
            log.error("Failed to truncate index: {}", e.getMessage());
            throw new ArktourosException(e, "Failed to truncate index");
        } finally {
            ElasticsearchClientPool.returnClient(esClient);
        }
    }
}

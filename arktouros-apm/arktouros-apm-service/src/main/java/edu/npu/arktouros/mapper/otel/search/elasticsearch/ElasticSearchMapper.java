package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.common.ElasticSearchIndex;
import edu.npu.arktouros.model.common.ResponseCodeEnum;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.PageResultVo;
import edu.npu.arktouros.model.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticSearch Mapper
 */
@Slf4j
public class ElasticSearchMapper extends SearchMapper {

    private final ElasticsearchClient esClient;

    public ElasticSearchMapper(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public R getServiceList(BaseQueryDto queryDto) {
        MatchQuery.Builder queryBuilder = new MatchQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(queryDto.query())) {
            queryBuilder.field("name").query(queryDto.query());
            Query matchQuery = queryBuilder.build()._toQuery();
            searchRequestBuilder.query(matchQuery);
        } else {
            // match_all
            searchRequestBuilder.query(new MatchAllQuery.Builder().build()._toQuery());
        }
        int pageSize = queryDto.pageSize();
        int pageNum = queryDto.pageNum();
        SearchRequest searchRequest =
                searchRequestBuilder
                        .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                        .from(pageSize * (pageNum - 1))
                        .size(pageSize).build();
        try {
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            return transformListResponseToR(searchResponse);
        } catch (IOException e) {
            log.error("Search for service list error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Service> getServiceListFromNamespace(String namespace) {
        MatchQuery.Builder queryBuilder = new MatchQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        queryBuilder.field("nameSpace").query(namespace);
        SearchRequest searchRequest = searchRequestBuilder
                .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                .query(queryBuilder.build()._toQuery()).build();
        try {
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            return searchResponse.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            log.error("Search for service with namespace error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Span> getSpanListByServiceNames(List<String> serviceNames) {
        TermsQuery.Builder termsQueryBuilder = new TermsQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        List<FieldValue> fieldValues = new ArrayList<>();
        for (String serviceName : serviceNames) {
            fieldValues.add(FieldValue.of(serviceName));
        }
        termsQueryBuilder
                .field("serviceName")
                .terms(builder -> builder.value(fieldValues));
        searchRequestBuilder
                .index(ElasticSearchIndex.SPAN_INDEX.getIndexName())
                .query(termsQueryBuilder.build()._toQuery());
        try {
            SearchResponse<Span> searchResponse =
                    esClient.search(searchRequestBuilder.build(), Span.class);
            return searchResponse.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            log.error("Search for span list error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Service getServiceByName(String serviceName) {
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        termQueryBuilder.field("name").value(serviceName);
        SearchRequest searchRequest = searchRequestBuilder
                .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                .query(termQueryBuilder.build()._toQuery()).build();
        try {
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            List<Hit<Service>> hits = searchResponse.hits().hits();
            if (hits.isEmpty()) {
                return null;
            }
            return hits.getFirst().source();
        } catch (IOException e) {
            log.error("Search for service by name error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public R getLogListByQuery(LogQueryDto logQueryDto) {
        MatchQuery.Builder matchQueryBuilder = new MatchQuery.Builder();
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        if (StringUtils.isNotEmpty(logQueryDto.serviceName())) {
            matchQueryBuilder.field("serviceName").query(logQueryDto.serviceName());
        }
        if (StringUtils.isNotEmpty(logQueryDto.traceId())) {
            termQueryBuilder.field("traceId").value(logQueryDto.traceId());
        }
        if (StringUtils.isNotEmpty(logQueryDto.keyword())) {
            matchQueryBuilder.field("content").query(logQueryDto.keyword());
        }
        boolQueryBuilder.must(
                termQueryBuilder.build()._toQuery(),
                matchQueryBuilder.build()._toQuery());
        if (StringUtils.isNotEmpty(logQueryDto.keywordNotIncluded())) {
            // content不包含
            boolQueryBuilder.filter(
                    new MatchQuery.Builder()
                            .field("content")
                            .query(logQueryDto.keywordNotIncluded())
                            .build()._toQuery()
            );
        }
        SearchRequest searchRequest = new SearchRequest.Builder()
                .query(boolQueryBuilder.build()._toQuery())
                .build();
        try {
            SearchResponse<Log> searchResponse =
                    esClient.search(searchRequest, Log.class);
            return transformListResponseToR(searchResponse);
        } catch (IOException e) {
            log.error("Search for log list error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private <T> R transformListResponseToR(SearchResponse<T> searchResponse) {
        R r = new R();
        r.put("code", ResponseCodeEnum.SUCCESS.getValue());
        List<Hit<T>> hits = searchResponse.hits().hits();
        long total = 0;
        if (searchResponse.hits().total() != null) {
            total = searchResponse.hits().total().value();
        }
        List<T> list = hits.stream().map(Hit::source).toList();
        PageResultVo<T> pageResult = new PageResultVo<>(total, list);
        r.put("data", pageResult);
        return r;
    }
}

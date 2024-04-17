package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.common.ElasticSearchIndex;
import edu.npu.arktouros.model.common.ResponseCodeEnum;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Counter;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.metric.Histogram;
import edu.npu.arktouros.model.otel.metric.Metric;
import edu.npu.arktouros.model.otel.metric.Summary;
import edu.npu.arktouros.model.otel.structure.EndPoint;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.EndPointTraceIdVo;
import edu.npu.arktouros.model.vo.PageResultVo;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.util.pool.ElasticsearchClientPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author : [wangminan]
 * @description : ElasticSearch Mapper
 */
@Slf4j
public class ElasticSearchMapper extends SearchMapper {

    @Override
    public R getServiceList(ServiceQueryDto queryDto) {
        MatchQuery.Builder queryBuilder = new MatchQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(queryDto.query())) {
            queryBuilder.field("name").query(queryDto.query());
            Query matchQuery = queryBuilder.build()._toQuery();
            searchRequestBuilder.query(matchQuery);
        }
        if (StringUtils.isNotEmpty(queryDto.namespace())) {
            queryBuilder.field("namespace").query(queryDto.namespace());
            Query matchQuery = queryBuilder.build()._toQuery();
            searchRequestBuilder.query(matchQuery);
        } else {
            queryBuilder.field("namespace").query("default");
            Query matchQuery = queryBuilder.build()._toQuery();
            searchRequestBuilder.query(matchQuery);
        }
        int pageSize = queryDto.pageSize();
        int pageNum = queryDto.pageNum();
        SearchRequest searchRequest =
                searchRequestBuilder
                        .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                        .from(pageSize * (pageNum - 1))
                        .size(pageSize).build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            ElasticsearchClientPool.returnClient(esClient);
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
        queryBuilder.field("namespace").query(namespace);
        SearchRequest searchRequest = searchRequestBuilder
                .index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                .query(queryBuilder.build()._toQuery()).build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            ElasticsearchClientPool.returnClient(esClient);
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
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Span> searchResponse =
                    esClient.search(searchRequestBuilder.build(), Span.class);
            ElasticsearchClientPool.returnClient(esClient);
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
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            ElasticsearchClientPool.returnClient(esClient);
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
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();

        SortOptions sort = new SortOptions.Builder()
                .field(new FieldSort.Builder()
                        .field("timestamp")
                        .order(SortOrder.Desc)
                        .build())
                .build();

        if (StringUtils.isNotEmpty(logQueryDto.serviceName())) {
            boolQueryBuilder.must(new MatchQuery.Builder()
                    .field("serviceName")
                    .query(logQueryDto.serviceName())
                    .build()._toQuery());
        }
        if (StringUtils.isNotEmpty(logQueryDto.traceId())) {
            boolQueryBuilder.must(new TermQuery.Builder()
                    .field("traceId")
                    .value(logQueryDto.traceId())
                    .build()._toQuery());
        }
        if (StringUtils.isNotEmpty(logQueryDto.keyword())) {
            boolQueryBuilder.must(new MatchQuery.Builder()
                    .field("content")
                    .query(logQueryDto.keyword())
                    .build()._toQuery());
        }
        if (StringUtils.isNotEmpty(logQueryDto.keywordNotIncluded())) {
            boolQueryBuilder.filter(new MatchQuery.Builder()
                    .field("content")
                    .query(logQueryDto.keywordNotIncluded()).build()._toQuery());
        }
        if (StringUtils.isNotEmpty(logQueryDto.severityText())) {
            boolQueryBuilder.filter(new TermQuery.Builder()
                    .field("severityText")
                    .value(logQueryDto.severityText())
                    .build()._toQuery());
        }
        if (logQueryDto.startTimestamp() != null) {
            RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
            rangeQueryBuilder.field("timestamp")
                    .gte(JsonData.of(logQueryDto.startTimestamp()));
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }
        if (logQueryDto.endTimestamp() != null) {
            RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
            rangeQueryBuilder.field("timestamp")
                    .lte(JsonData.of(logQueryDto.endTimestamp()));
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }

        searchRequestBuilder
                .index(ElasticSearchIndex.LOG_INDEX.getIndexName())
                .query(boolQueryBuilder.build()._toQuery())
                .from(logQueryDto.pageSize() * (logQueryDto.pageNum() - 1))
                .size(logQueryDto.pageSize())
                .sort(sort);

        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Log> searchResponse = esClient.search(
                    searchRequestBuilder.build(), Log.class);
            ElasticsearchClientPool.returnClient(esClient);
            return transformListResponseToR(searchResponse);
        } catch (IOException e) {
            log.error("Search for log list error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto) {
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();
        termQueryBuilder
                .field("serviceName")
                .value(endPointQueryDto.serviceName());
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ElasticSearchIndex.SPAN_INDEX.getIndexName())
                .query(termQueryBuilder.build()._toQuery())
                .from(endPointQueryDto.pageSize() * (endPointQueryDto.pageNum() - 1))
                .size(endPointQueryDto.pageSize())
                .build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Span> searchResponse =
                    esClient.search(searchRequest, Span.class);
            ElasticsearchClientPool.returnClient(esClient);
            List<Hit<Span>> hits = searchResponse.hits().hits();
            R r = new R();
            r.put("code", ResponseCodeEnum.SUCCESS.getValue());
            Set<EndPoint> endPointSet = new HashSet<>();
            List<EndPointTraceIdVo> endPointTraceIdVoList = new ArrayList<>();
            if (hits.isEmpty()) {
                r.put("result", new ArrayList<>());
            } else {
                hits.forEach(hit -> {
                    if (hit.source() != null) {
                        EndPoint localEndPoint = hit.source().getLocalEndPoint();
                        if (endPointSet.contains(localEndPoint)) {
                            // endPointTraceIdDtoList中找到对应记录 并在traceIds中做添加
                            for (EndPointTraceIdVo endPointTraceIdVo :
                                    endPointTraceIdVoList) {
                                if (endPointTraceIdVo.endPoint().equals(localEndPoint)) {
                                    endPointTraceIdVo
                                            .traceIds()
                                            .add(hit.source().getTraceId());
                                    break;
                                }
                            }
                        } else {
                            endPointSet.add(localEndPoint);
                            EndPointTraceIdVo endPointTraceIdVo =
                                    new EndPointTraceIdVo(localEndPoint,
                                            new HashSet<>());
                            endPointTraceIdVo.traceIds().add(hit.source().getTraceId());
                            endPointTraceIdVoList.add(endPointTraceIdVo);
                        }
                    }
                });
                r.put("result", endPointTraceIdVoList);
            }
            return r;
        } catch (IOException e) {
            log.error("Search for traceId list by service name error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Span> getSpanListByTraceId(String traceId) {
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();
        termQueryBuilder.field("traceId").value(traceId);
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ElasticSearchIndex.SPAN_INDEX.getIndexName())
                .query(termQueryBuilder.build()._toQuery())
                .build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Span> searchResponse =
                    esClient.search(searchRequest, Span.class);
            ElasticsearchClientPool.returnClient(esClient);
            return searchResponse.hits().hits().stream().map(Hit::source).toList();
        } catch (IOException e) {
            log.error("Search for span list by trace query error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getMetricsNames(String serviceName, Integer metricNameLimit) {
        try {
            List<String> allNames = new ArrayList<>();
            allNames.addAll(getMetricNamesFromIndex(serviceName,
                    ElasticSearchIndex.GAUGE_INDEX.getIndexName(), Gauge.class));
            allNames.addAll(getMetricNamesFromIndex(serviceName,
                    ElasticSearchIndex.COUNTER_INDEX.getIndexName(), Counter.class));
            allNames.addAll(getMetricNamesFromIndex(serviceName,
                    ElasticSearchIndex.HISTOGRAM_INDEX.getIndexName(), Histogram.class));
            allNames.addAll(getMetricNamesFromIndex(serviceName,
                    ElasticSearchIndex.SUMMARY_INDEX.getIndexName(), Summary.class));

            return metricNameLimit == null ? allNames : allNames.stream().limit(metricNameLimit).toList();
        } catch (IOException e) {
            log.error("Search for metric names error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private <T extends Metric> List<String> getMetricNamesFromIndex(
            String serviceName, String indexName, Class<T> clazz) throws IOException {
        MatchQuery.Builder matchQueryBuilder = new MatchQuery.Builder();
        matchQueryBuilder.field("serviceName").query(serviceName);
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(indexName).query(matchQueryBuilder.build()._toQuery());
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        SearchResponse<T> searchResponse = esClient.search(searchRequestBuilder.build(), clazz);
        ElasticsearchClientPool.returnClient(esClient);
        return searchResponse.hits().hits()
                .stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> hit.source().getName())
                .collect(Collectors.toList());
    }

    @Override
    public List<Metric> getMetricsValues(List<String> metricNames,
                                         Long startTimestamp, Long endTimestamp) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        TermsQuery.Builder termQueryBuilder = new TermsQuery.Builder();
        List<FieldValue> fieldValues = new ArrayList<>();
        for (String metricName : metricNames) {
            fieldValues.add(FieldValue.of(metricName));
        }
        termQueryBuilder.field("name").terms(builder -> builder.value(fieldValues));
        boolQueryBuilder.must(termQueryBuilder.build()._toQuery());
        if (startTimestamp != null) {
            RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
            rangeQueryBuilder.field("timestamp")
                    .gte(JsonData.of(startTimestamp));
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }
        if (endTimestamp != null) {
            RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
            rangeQueryBuilder.field("timestamp")
                    .lte(JsonData.of(endTimestamp));
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }
        Query query = boolQueryBuilder.build()._toQuery();
        List<Metric> metrics = new ArrayList<>();
        try {
            metrics.addAll(getMetricsFromBoolQuery(ElasticSearchIndex.GAUGE_INDEX.getIndexName(),
                    query, Gauge.class));
            metrics.addAll(getMetricsFromBoolQuery(ElasticSearchIndex.COUNTER_INDEX.getIndexName(),
                    query, Counter.class));
            metrics.addAll(getMetricsFromBoolQuery(ElasticSearchIndex.HISTOGRAM_INDEX.getIndexName(),
                    query, Histogram.class));
            metrics.addAll(getMetricsFromBoolQuery(ElasticSearchIndex.SUMMARY_INDEX.getIndexName(),
                    query, Summary.class));
        } catch (IOException e) {
            log.error("Search for metric values error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
        return metrics;
    }

    @Override
    public R getNamespaceList(String query) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(query)) {
            searchRequestBuilder.query(new Query.Builder()
                    .prefix(prefixBuilder ->
                            prefixBuilder.field("namespace")
                                    .value(query))
                    .build()
            );
        } else {
            // group by
            searchRequestBuilder.aggregations("namespaceAgg",
                    agg -> agg.terms(term -> term.field("namespace")));
        }
        searchRequestBuilder.index(ElasticSearchIndex.SERVICE_INDEX.getIndexName())
                .size(10);
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequestBuilder.build(), Service.class);
            ElasticsearchClientPool.returnClient(esClient);
            List<String> namespaceList = new ArrayList<>();
            if (StringUtils.isNotEmpty(query)) {
                namespaceList =
                        searchResponse.hits().hits().stream()
                                .map(hit -> {
                                    if (hit.source() != null) {
                                        return hit.source().getNamespace();
                                    }
                                    return null;
                                })
                                .distinct()
                                .toList();
            } else {
                Aggregate namespaceAgg = searchResponse.aggregations().get("namespaceAgg");
                List<StringTermsBucket> buckets = namespaceAgg.sterms().buckets().array();
                for (StringTermsBucket bucket : buckets) {
                    namespaceList.add(bucket.key().stringValue());
                }
            }
            R r = new R();
            r.put("result", namespaceList);
            return r;
        } catch (IOException e) {
            log.error("Search for namespace list error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private <T extends Metric> List<Metric> getMetricsFromBoolQuery(
            String indexName, Query query, Class<T> clazz) throws IOException {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(indexName).query(query);
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        SearchResponse<T> searchResponse =
                esClient.search(searchRequestBuilder.build(), clazz);
        ElasticsearchClientPool.returnClient(esClient);
        return searchResponse.hits().hits()
                .stream()
                .filter(hit -> hit.source() != null)
                .map(Hit::source)
                .collect(Collectors.toList());
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
        r.put("result", pageResult);
        return r;
    }
}

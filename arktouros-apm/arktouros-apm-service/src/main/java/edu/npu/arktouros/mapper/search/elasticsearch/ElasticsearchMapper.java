package edu.npu.arktouros.mapper.search.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeAggregationSource;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeBucket;
import co.elastic.clients.elasticsearch._types.aggregations.CompositeDateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TopHitsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.common.ElasticsearchConstants;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.common.ResponseCodeEnum;
import edu.npu.arktouros.model.dto.EndPointQueryDto;
import edu.npu.arktouros.model.dto.LogQueryDto;
import edu.npu.arktouros.model.dto.ServiceQueryDto;
import edu.npu.arktouros.model.dto.SpanTopologyQueryDto;
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
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : [wangminan]
 * @description : ElasticSearch Mapper
 */
@Slf4j
public class ElasticsearchMapper extends SearchMapper {
    private static final String RESULT = "result";
    private static final String SERVICE_NAME = "serviceName";
    private static final String NAMESPACE = "namespace";
    private static final String TIMESTAMP = "timestamp";
    private static final String SEVERITY_TEXT = "severityText";

    @Override
    public R getServiceList(ServiceQueryDto queryDto) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        MatchQuery.Builder matchQueryBuilder = new MatchQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(queryDto.query())) {
            boolQueryBuilder.must(new Query.Builder().prefix(prefixBuilder ->
                    prefixBuilder.field("name")
                            .value(queryDto.query())).build());
        }
        if (StringUtils.isNotEmpty(queryDto.namespace())) {
            matchQueryBuilder.field(NAMESPACE).query(queryDto.namespace());
        } else {
            matchQueryBuilder.field(NAMESPACE).query("default");
        }
        boolQueryBuilder.must(matchQueryBuilder.build()._toQuery());
        searchRequestBuilder.query(boolQueryBuilder.build()._toQuery());

        int pageSize = queryDto.pageSize();
        int pageNum = queryDto.pageNum();
        searchRequestBuilder
                .index(ElasticsearchIndex.SERVICE_INDEX.getIndexName())
                .from(pageSize * (pageNum - 1))
                .size(pageSize);
        SearchResponse<Service> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Service.class);
        return transformListResponseToR(searchResponse);
    }

    @Override
    public List<Service> getServiceListFromNamespace(String namespace) {
        MatchQuery.Builder queryBuilder = new MatchQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        queryBuilder.field(NAMESPACE).query(namespace);
        searchRequestBuilder
                .index(ElasticsearchIndex.SERVICE_INDEX.getIndexName())
                .query(queryBuilder.build()._toQuery())
                .size(ElasticsearchConstants.MAX_PAGE_SIZE);
        SearchResponse<Service> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Service.class);
        return searchResponse.hits().hits().stream().map(Hit::source).toList();
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
                .field(SERVICE_NAME)
                .terms(builder -> builder.value(fieldValues));
        searchRequestBuilder
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(termsQueryBuilder.build()._toQuery());
        return ElasticsearchUtil.scrollSearch(searchRequestBuilder, Span.class);
    }

    @Override
    public Service getServiceByName(String serviceName) {
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();

        termQueryBuilder.field("name").value(serviceName);
        SearchRequest.Builder searchRequestBuilder =
                new SearchRequest.Builder()
                        .index(ElasticsearchIndex.SERVICE_INDEX.getIndexName())
                        .query(termQueryBuilder.build()._toQuery());
        SearchResponse<Service> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Service.class);
        List<Hit<Service>> hits = searchResponse.hits().hits();
        if (hits.isEmpty()) {
            return null;
        }
        return hits.getFirst().source();
    }

    @Override
    public R getLogListByQuery(LogQueryDto logQueryDto) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        log.debug(logQueryDto.toString());
        SortOptions sort = new SortOptions.Builder()
                .field(new FieldSort.Builder()
                        .field(TIMESTAMP)
                        .order(SortOrder.Desc)
                        .build())
                .build();

        if (StringUtils.isNotEmpty(logQueryDto.serviceName())) {
            if (logQueryDto.serviceName().equals("null")) {
                boolQueryBuilder.mustNot(
                        new ExistsQuery.Builder()
                                .field(SERVICE_NAME)
                                .build()._toQuery()
                );
            } else {
                boolQueryBuilder.must(new MatchQuery.Builder()
                        .field(SERVICE_NAME)
                        .query(logQueryDto.serviceName())
                        .build()._toQuery());
            }
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
            boolQueryBuilder.mustNot(new MatchQuery.Builder()
                    .field("content")
                    .query(logQueryDto.keywordNotIncluded()).build()._toQuery());
        }
        if (StringUtils.isNotEmpty(logQueryDto.severityText())) {
            if (!logQueryDto.severityText().equals("null")) {
                boolQueryBuilder.filter(new TermQuery.Builder()
                        .field(SEVERITY_TEXT)
                        .value(logQueryDto.severityText())
                        .build()._toQuery());
            } else {
                boolQueryBuilder.filter(new TermQuery.Builder()
                        .field(SEVERITY_TEXT)
                        .value("")
                        .build()._toQuery());
            }
        }
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        if (logQueryDto.startTimestamp() != null) {
            rangeQueryBuilder.field(TIMESTAMP)
                    .gte(JsonData.of(logQueryDto.startTimestamp()));
        }
        if (logQueryDto.endTimestamp() != null) {
            rangeQueryBuilder.field(TIMESTAMP)
                    .lte(JsonData.of(logQueryDto.endTimestamp()));
        }

        if (logQueryDto.startTimestamp() != null || logQueryDto.endTimestamp() != null) {
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }

        searchRequestBuilder
                .index(ElasticsearchIndex.LOG_INDEX.getIndexName())
                .query(boolQueryBuilder.build()._toQuery())
                .from(logQueryDto.pageSize() * (logQueryDto.pageNum() - 1))
                .size(logQueryDto.pageSize())
                .sort(sort);

        SearchResponse<Log> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Log.class);
        return transformListResponseToR(searchResponse);
    }

    @Override
    public R getEndPointListByServiceName(EndPointQueryDto endPointQueryDto) {
        List<EndPointTraceIdVo> endPointTraceIdVoList = getEndPointTraceIdVos(endPointQueryDto);
        R r = new R();
        r.put(RESULT, endPointTraceIdVoList);
        return r;
    }

    @Override
    public List<EndPointTraceIdVo> getEndPointTraceIdVos(EndPointQueryDto endPointQueryDto) {
        TermQuery.Builder termQueryBuilder = new TermQuery.Builder();
        termQueryBuilder
                .field(SERVICE_NAME)
                .value(endPointQueryDto.serviceName());
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(termQueryBuilder.build()._toQuery())
                .from(endPointQueryDto.pageSize() * (endPointQueryDto.pageNum() - 1))
                .size(endPointQueryDto.pageSize());
        SearchResponse<Span> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Span.class);
        List<Hit<Span>> hits = searchResponse.hits().hits();
        Set<EndPoint> endPointSet = new HashSet<>();
        List<EndPointTraceIdVo> endPointTraceIdVoList = new ArrayList<>();

        hits.forEach(hit -> resolveHit(hit, endPointSet, endPointTraceIdVoList));
        return endPointTraceIdVoList;
    }

    private static void resolveHit(Hit<Span> hit, Set<EndPoint> endPointSet, List<EndPointTraceIdVo> endPointTraceIdVoList) {
        if (hit.source() != null) {
            EndPoint localEndPoint = hit.source().getLocalEndPoint();
            EndPoint remoteEndPoint = hit.source().getRemoteEndPoint();
            if (localEndPoint != null && endPointSet.contains(localEndPoint)) {
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
            } else if (localEndPoint != null || endPointSet.isEmpty()) {
                endPointSet.add(localEndPoint);
                EndPointTraceIdVo endPointTraceIdVo =
                        new EndPointTraceIdVo(localEndPoint,
                                new HashSet<>());
                endPointTraceIdVo.traceIds().add(hit.source().getTraceId());
                endPointTraceIdVoList.add(endPointTraceIdVo);
            } else if (remoteEndPoint == null) {
                // all null
                for (EndPointTraceIdVo endPointTraceIdVo :
                        endPointTraceIdVoList) {
                    if (endPointTraceIdVo.endPoint() == null) {
                        endPointTraceIdVo
                                .traceIds()
                                .add(hit.source().getTraceId());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public List<Span> getSpanListByTraceId(SpanTopologyQueryDto spanTopologyQueryDto) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        if (spanTopologyQueryDto.innerService()) {
            boolQueryBuilder.must(
                    new TermQuery.Builder()
                        .field("traceId")
                        .value(spanTopologyQueryDto.traceId())
                        .build()._toQuery(),
                    new TermQuery.Builder()
                            .field("serviceName")
                            .value(spanTopologyQueryDto.serviceName())
                            .build()._toQuery()
            );
        } else {
            boolQueryBuilder.must(
                new TermQuery.Builder()
                        .field("traceId")
                        .value(spanTopologyQueryDto.traceId())
                        .build()._toQuery()
        );
        }
        Query query = boolQueryBuilder.build()._toQuery();
        log.debug("Bool query for get span: {}, innerService: {}", query, spanTopologyQueryDto.innerService());
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(query);
        return ElasticsearchUtil.scrollSearch(searchRequestBuilder, Span.class);
    }

    @Override
    public List<String> getMetricsNames(String serviceName, Integer metricNameLimit) {
        List<String> allNames = new ArrayList<>();
        allNames.addAll(getMetricNamesFromIndex(serviceName,
                ElasticsearchIndex.GAUGE_INDEX.getIndexName(), Gauge.class));
        allNames.addAll(getMetricNamesFromIndex(serviceName,
                ElasticsearchIndex.COUNTER_INDEX.getIndexName(), Counter.class));
        allNames.addAll(getMetricNamesFromIndex(serviceName,
                ElasticsearchIndex.HISTOGRAM_INDEX.getIndexName(), Histogram.class));
        allNames.addAll(getMetricNamesFromIndex(serviceName,
                ElasticsearchIndex.SUMMARY_INDEX.getIndexName(), Summary.class));
        return metricNameLimit == null ? allNames : allNames.stream().limit(metricNameLimit).toList();
    }

    private <T extends Metric> List<String> getMetricNamesFromIndex(
            String serviceName, String indexName, Class<T> clazz) {
        MatchQuery.Builder matchQueryBuilder = new MatchQuery.Builder();
        matchQueryBuilder.field(SERVICE_NAME).query(serviceName);
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder
                .index(indexName)
                .query(matchQueryBuilder.build()._toQuery())
                .aggregations("nameAgg",
                        agg -> agg.terms(term -> term.field("name")));
        searchRequestBuilder.size(ElasticsearchConstants.MAX_PAGE_SIZE);
        SearchResponse<T> searchResponse = ElasticsearchUtil
                .simpleSearch(searchRequestBuilder, clazz);
        Aggregate namespaceAgg = searchResponse.aggregations().get("nameAgg");
        List<StringTermsBucket> buckets = namespaceAgg.sterms().buckets().array();
        return buckets.stream().map(bucket -> bucket.key().stringValue()).toList();
    }

    @Override
    public List<Metric> getMetricsValues(List<String> metricNames, String serviceName,
                                         Long startTimestamp, Long endTimestamp) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        TermsQuery.Builder termQueryBuilder = new TermsQuery.Builder();
        List<FieldValue> fieldValues = new ArrayList<>();
        for (String metricName : metricNames) {
            fieldValues.add(FieldValue.of(metricName));
        }
        termQueryBuilder.field("name").terms(builder -> builder.value(fieldValues));
        boolQueryBuilder.must(termQueryBuilder.build()._toQuery());
        boolQueryBuilder.must(new TermQuery.Builder()
                .field(SERVICE_NAME).value(serviceName).build()._toQuery());
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        if (startTimestamp != null) {
            rangeQueryBuilder.field(TIMESTAMP)
                    .gte(JsonData.of(startTimestamp));
        }
        if (endTimestamp != null) {
            rangeQueryBuilder.field(TIMESTAMP)
                    .lte(JsonData.of(endTimestamp));
        }
        if (startTimestamp != null || endTimestamp != null) {
            boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        }
        Query query = boolQueryBuilder.build()._toQuery();
        List<Metric> metrics = new ArrayList<>();
        metrics.addAll(getMetricsFromBoolQuery(
                startTimestamp, endTimestamp,
                ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                query, Gauge.class));
        metrics.addAll(getMetricsFromBoolQuery(
                startTimestamp, endTimestamp,
                ElasticsearchIndex.COUNTER_INDEX.getIndexName(),
                query, Counter.class));
        metrics.addAll(getMetricsFromBoolQuery(
                startTimestamp, endTimestamp,
                ElasticsearchIndex.HISTOGRAM_INDEX.getIndexName(),
                query, Histogram.class));
        metrics.addAll(getMetricsFromBoolQuery(
                startTimestamp, endTimestamp,
                ElasticsearchIndex.SUMMARY_INDEX.getIndexName(),
                query, Summary.class));
        return metrics;
    }

    @Override
    public R getNamespaceList(String query) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(query)) {
            searchRequestBuilder.query(new Query.Builder()
                    .prefix(prefixBuilder ->
                            prefixBuilder.field(NAMESPACE)
                                    .value(query))
                    .build()
            );
        } else {
            // group by
            searchRequestBuilder.aggregations("namespaceAgg",
                    agg -> agg.terms(term -> term.field(NAMESPACE)));
        }
        searchRequestBuilder.index(ElasticsearchIndex.SERVICE_INDEX.getIndexName());
        List<String> namespaceList = new ArrayList<>();
        if (StringUtils.isNotEmpty(query)) {
            List<Service> serviceList =
                    ElasticsearchUtil.scrollSearch(searchRequestBuilder, Service.class);
            namespaceList =
                    serviceList.stream()
                            .map(hit -> {
                                if (hit != null) {
                                    return hit.getNamespace();
                                }
                                return null;
                            })
                            .distinct()
                            .toList();
        } else {
            searchRequestBuilder.size(ElasticsearchConstants.MAX_PAGE_SIZE);
            SearchResponse<Service> searchResponse = ElasticsearchUtil
                    .simpleSearch(searchRequestBuilder, Service.class);
            Aggregate namespaceAgg = searchResponse.aggregations().get("namespaceAgg");
            List<StringTermsBucket> buckets = namespaceAgg.sterms().buckets().array();
            for (StringTermsBucket bucket : buckets) {
                namespaceList.add(bucket.key().stringValue());
            }
        }
        R r = new R();
        r.put(RESULT, namespaceList);
        return r;
    }

    @Override
    public R getAllLogLevels(String query) {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (StringUtils.isNotEmpty(query)) {
            searchRequestBuilder.query(new Query.Builder()
                    .prefix(prefixBuilder ->
                            prefixBuilder.field(SEVERITY_TEXT)
                                    .value(query))
                    .build()
            );
        } else {
            // group by
            searchRequestBuilder.aggregations("severityAgg",
                    agg -> agg.terms(term -> term.field(SEVERITY_TEXT)));
        }
        List<String> severityList = new ArrayList<>();
        searchRequestBuilder.index(ElasticsearchIndex.LOG_INDEX.getIndexName());
        if (StringUtils.isNotEmpty(query)) {
            List<Log> logList =
                    ElasticsearchUtil.scrollSearch(searchRequestBuilder, Log.class);
            severityList =
                    logList.stream()
                            .map(hit -> {
                                if (hit != null) {
                                    return hit.getSeverityText();
                                }
                                return null;
                            })
                            .distinct()
                            .toList();
        } else {
            searchRequestBuilder.size(ElasticsearchConstants.MAX_PAGE_SIZE);
            SearchResponse<Log> searchResponse =
                    ElasticsearchUtil.simpleSearch(searchRequestBuilder, Log.class);
            Aggregate severityAgg = searchResponse.aggregations().get("severityAgg");
            List<StringTermsBucket> buckets = severityAgg.sterms().buckets().array();
            for (StringTermsBucket bucket : buckets) {
                severityList.add(bucket.key().stringValue());
            }
        }
        R r = new R();
        r.put(RESULT, severityList);
        return r;
    }

    @Override
    public List<Service> getAllServices() {
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(ElasticsearchIndex.SERVICE_INDEX.getIndexName());
        return ElasticsearchUtil.scrollSearch(searchRequestBuilder, Service.class);
    }

    @Override
    public int getTraceCount(Service service, long startTime, long endTime) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.must(new TermQuery.Builder()
                .field(SERVICE_NAME)
                .value(service.getName())
                .build()._toQuery());
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(TIMESTAMP)
                .gte(JsonData.of(startTime))
                .lte(JsonData.of(endTime));
        boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(boolQueryBuilder.build()._toQuery())
                .aggregations("traceIdAgg",
                        agg -> agg.terms(term -> term.field("traceId")));
        searchRequestBuilder.size(ElasticsearchConstants.MAX_PAGE_SIZE);
        SearchResponse<Span> searchResponse =
                ElasticsearchUtil.simpleSearch(searchRequestBuilder, Span.class);
        Aggregate traceIdAgg = searchResponse.aggregations().get("traceIdAgg");
        return traceIdAgg.sterms().buckets().array().size();
    }

    @Override
    public List<Span> getSpanListByTraceId(String serviceName, String traceId,
                                           long startTime, long endTime) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.must(new TermQuery.Builder()
                .field(SERVICE_NAME)
                .value(serviceName)
                .build()._toQuery());
        boolQueryBuilder.must(new TermQuery.Builder()
                .field("traceId")
                .value(traceId)
                .build()._toQuery());
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(TIMESTAMP)
                .gte(JsonData.of(startTime))
                .lte(JsonData.of(endTime));
        boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(boolQueryBuilder.build()._toQuery());
        return ElasticsearchUtil.scrollSearch(searchRequestBuilder, Span.class);
    }

    @Override
    public List<Span> getAllSpans(Service service, long startTime) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.must(new TermQuery.Builder()
                .field(SERVICE_NAME)
                .value(service.getName())
                .build()._toQuery());
        RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder();
        rangeQueryBuilder.field(TIMESTAMP)
                .gte(JsonData.of(startTime));
        boolQueryBuilder.must(rangeQueryBuilder.build()._toQuery());
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(boolQueryBuilder.build()._toQuery());
        return ElasticsearchUtil.scrollSearch(searchRequestBuilder, Span.class);
    }

    private <T extends Metric> List<T> getMetricsFromBoolQuery(
            Long startTimestamp, Long endTimestamp,
            String indexName, Query query, Class<T> clazz) {
        SortOptions sort = new SortOptions.Builder()
                .field(new FieldSort.Builder()
                        .field(TIMESTAMP)
                        .order(SortOrder.Asc)
                        .build())
                .build();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index(indexName)
                .query(query)
                .sort(sort);
        boolean samplerQuery = false;
        if (startTimestamp != null && endTimestamp != null) {
            samplerQuery = samplerQuery(searchRequestBuilder, startTimestamp, endTimestamp);
        }
        if (samplerQuery) {
            return metricAggSearch(clazz, searchRequestBuilder);
        } else {
            return ElasticsearchUtil.scrollSearch(searchRequestBuilder, clazz);
        }
    }

    private <T extends Metric> List<T> metricAggSearch(Class<T> clazz, SearchRequest.Builder searchRequestBuilder) {
        SearchResponse<T> searchResponse = ElasticsearchUtil.simpleSearch(searchRequestBuilder, clazz);
        Aggregate timeSamplerAgg = searchResponse.aggregations().get("timeSamplerAgg");
        List<CompositeBucket> timeSamplerAggBuckets =
                timeSamplerAgg.composite().buckets().array();
        List<T> result = new ArrayList<>();
        for (CompositeBucket bucket : timeSamplerAggBuckets) {
            Aggregate nameAgg = bucket.aggregations().get("nameAgg");
            List<StringTermsBucket> nameAggBuckets =
                    nameAgg.sterms().buckets().array();
            nameAggBuckets.forEach(nameAggBucket -> {
                Aggregate topDocAgg = nameAggBucket.aggregations().get("topDocAgg");
                topDocAgg.topHits().hits().hits().forEach(topDoc -> {
                    if (topDoc.source() != null) {
                        T t = topDoc.source().to(clazz);
                        result.add(t);
                    }
                });
            });
        }
        return result;
    }

    private boolean samplerQuery(SearchRequest.Builder searchRequestBuilder, Long startTimestamp, Long endTimestamp) {
        // 如果两个时间戳相差的时间在一个小时之内 return
        if (endTimestamp - startTimestamp < 3600000) {
            return false; // 全采样
        }
        CompositeDateHistogramAggregation compositeDateHistogramAggregation;
        if (endTimestamp - startTimestamp <= 86400000) {
            // 如果两个时间戳相差的时间在一天之内 以小时为单位做聚合
            compositeDateHistogramAggregation =
                    new CompositeDateHistogramAggregation.Builder()
                            .field("timestamp")
                            .calendarInterval(new Time.Builder().time("1h").build())
                            .build();
        } else if (endTimestamp - startTimestamp <= 2592000000L) {
            // 如果两个时间戳相差的时间在一月之内 以一天为单位做聚合
            compositeDateHistogramAggregation =
                    new CompositeDateHistogramAggregation.Builder()
                            .field("timestamp")
                            .calendarInterval(new Time.Builder().time("1d").build())
                            .build();
        } else {
            // 以一年为单位做聚合
            compositeDateHistogramAggregation =
                    new CompositeDateHistogramAggregation.Builder()
                            .field("timestamp")
                            .calendarInterval(new Time.Builder().time("1y").build())
                            .build();
        }
        Map<String, CompositeAggregationSource> timestampMap =
                Map.of("timestamp", new CompositeAggregationSource.Builder()
                        .dateHistogram(compositeDateHistogramAggregation)
                        .build());
        List<Map<String, CompositeAggregationSource>> timestampList = new ArrayList<>();
        timestampList.add(timestampMap);
        // 只去头部的doc作为采样点 虽然这样很不严谨 但就这样算了
        Aggregation topDocAgg = new Aggregation.Builder()
                .topHits(new TopHitsAggregation.Builder()
                        .size(1)
                        .build())
                .build();
        Aggregation nameAgg = new Aggregation.Builder()
                .terms(new TermsAggregation.Builder()
                        .field("name")
                        .build())
                .aggregations("topDocAgg", topDocAgg)
                .build();
        Aggregation timeSamplerAgg = new Aggregation.Builder()
                .composite(
                        new CompositeAggregation.Builder()
                                .sources(timestampList)
                                .build())
                .aggregations("nameAgg", nameAgg)
                .build();
        searchRequestBuilder.aggregations("timeSamplerAgg", timeSamplerAgg);
        return true;
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
        r.put(RESULT, pageResult);
        return r;
    }
}

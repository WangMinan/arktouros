package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.common.ResponseCodeEnum;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.vo.PageResultVo;
import edu.npu.arktouros.model.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
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
        if (StringUtils.isNotEmpty(queryDto.query())) {
            queryBuilder.field("name").query(queryDto.query());
        }
        Query matchQuery = queryBuilder.build()._toQuery();
        int pageSize = queryDto.pageSize();
        int pageNum = queryDto.pageNum();
        SearchRequest searchRequest = new SearchRequest.Builder()
                .query(matchQuery)
                .from(pageSize * (pageNum - 1))
                .size(pageSize)
                .build();
        try {
            SearchResponse<Service> searchResponse =
                    esClient.search(searchRequest, Service.class);
            return transformListResponseToR(searchResponse);
        } catch (IOException e) {
            log.error("Search for service error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private R transformListResponseToR(SearchResponse<Service> searchResponse) {
        R r = new R();
        r.put("code", ResponseCodeEnum.SUCCESS);
        List<Hit<Service>> hits = searchResponse.hits().hits();
        long total = 0;
        if (searchResponse.hits().total() != null) {
            total = searchResponse.hits().total().value();
        }
        List<Service> list = hits.stream().map(Hit::source).toList();
        PageResultVo<Service> pageResult = new PageResultVo<>(total, list);
        r.put("data", pageResult);
        return r;
    }
}

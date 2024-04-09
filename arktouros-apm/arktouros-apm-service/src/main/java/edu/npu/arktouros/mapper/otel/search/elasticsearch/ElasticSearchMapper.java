package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.mapper.otel.search.SearchMapper;
import edu.npu.arktouros.model.dto.BaseQueryDto;
import edu.npu.arktouros.model.otel.structure.Service;

import java.util.List;

/**
 * @author : [wangminan]
 * @description : ElasticSearch Mapper
 */
public class ElasticSearchMapper extends SearchMapper {

    private final ElasticsearchClient esClient;

    public ElasticSearchMapper(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public List<Service> getServiceList(BaseQueryDto queryDto) {
        return List.of();
    }
}

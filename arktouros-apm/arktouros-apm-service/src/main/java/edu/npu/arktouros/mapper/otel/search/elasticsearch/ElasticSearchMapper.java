package edu.npu.arktouros.mapper.otel.search.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.mapper.otel.search.SearchMapper;

/**
 * @author : [wangminan]
 * @description : ElasticSearch Mapper
 */
public class ElasticSearchMapper extends SearchMapper {

    private final ElasticsearchClient esClient;

    public ElasticSearchMapper(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }
}

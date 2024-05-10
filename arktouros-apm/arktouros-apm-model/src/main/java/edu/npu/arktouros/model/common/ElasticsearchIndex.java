package edu.npu.arktouros.model.common;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : Elasticsearch索引常量
 */
@Getter
public enum ElasticsearchIndex {
    SERVICE_INDEX("arktouros-service"),
    LOG_INDEX("arktouros-log"),
    SPAN_INDEX("arktouros-span"),
    GAUGE_INDEX("arktouros-gauge"),
    COUNTER_INDEX("arktouros-counter"),
    SUMMARY_INDEX("arktouros-summary"),
    HISTOGRAM_INDEX("arktouros-histogram");

    private final String indexName;

    ElasticsearchIndex(String indexName) {
        this.indexName = indexName;
    }

    public static List<String> getIndexList() {
        return Arrays.stream(ElasticsearchIndex.values())
                .map(ElasticsearchIndex::getIndexName)
                .toList();
    }
}

package edu.npu.arktouros.model.common;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : [wangminan]
 * @description : ElasticSearch索引常量
 */
@Getter
public enum ElasticSearchIndex {
    SERVICE_INDEX("arktouros-service"),
    LOG_INDEX("arktouros-log"),
    SPAN_INDEX("arktouros-span"),
    GAUGE_INDEX("arktouros-gauge"),
    COUNTER_INDEX("arktouros-counter"),
    SUMMARY_INDEX("arktouros-summary"),
    HISTOGRAM_INDEX("arktouros-histogram");

    private final String indexName;

    ElasticSearchIndex(String indexName) {
        this.indexName = indexName;
    }

    public static List<String> getIndexList() {
        return Arrays.stream(ElasticSearchIndex.values())
                .map(ElasticSearchIndex::getIndexName)
                .collect(Collectors.toList());
    }
}

package edu.npu.arktouros.service.operation.elasticsearch;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;

import java.util.List;

import static edu.npu.arktouros.model.common.ElasticsearchIndex.COUNTER_INDEX;
import static edu.npu.arktouros.model.common.ElasticsearchIndex.GAUGE_INDEX;
import static edu.npu.arktouros.model.common.ElasticsearchIndex.HISTOGRAM_INDEX;
import static edu.npu.arktouros.model.common.ElasticsearchIndex.LOG_INDEX;
import static edu.npu.arktouros.model.common.ElasticsearchIndex.SPAN_INDEX;
import static edu.npu.arktouros.model.common.ElasticsearchIndex.SUMMARY_INDEX;

/**
 * @author : [wangminan]
 * @description : Elasticsearch数据运维服务
 */
public class ElasticsearchOperationService extends DataOperationService {

    public ElasticsearchOperationService(DataReceiver dataReceiver) {
        super(dataReceiver);
    }

    @Override
    public void deleteAllData() {
        dataReceiver.stopAndClean();
        List<String> allIndexes = ElasticsearchUtil.getAllArktourosIndexes();
        ElasticsearchUtil.truncateIndexes(allIndexes);
        dataReceiver.flushAndStart();
    }

    @Override
    public void deleteAllLogs() {
        dataReceiver.stopAndClean();
        List<String> allLogIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(LOG_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allLogIndexes);
        dataReceiver.flushAndStart();
    }

    @Override
    public void deleteAllSpans() {
        dataReceiver.stopAndClean();
        List<String> allSpanIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(SPAN_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allSpanIndexes);
        dataReceiver.flushAndStart();
    }

    @Override
    public void deleteAllMetrics() {
        dataReceiver.stopAndClean();
        List<String> allMetricIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(GAUGE_INDEX.getIndexName())||
                        index.startsWith(COUNTER_INDEX.getIndexName()) ||
                        index.startsWith(HISTOGRAM_INDEX.getIndexName()) ||
                        index.startsWith(SUMMARY_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allMetricIndexes);
        dataReceiver.flushAndStart();
    }
}

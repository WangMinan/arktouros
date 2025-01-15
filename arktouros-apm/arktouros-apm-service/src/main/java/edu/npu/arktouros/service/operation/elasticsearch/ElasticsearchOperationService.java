package edu.npu.arktouros.service.operation.elasticsearch;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ElasticsearchOperationService extends DataOperationService {

    public ElasticsearchOperationService(DataReceiver dataReceiver, ScheduledJob scheduledJob) {
        super(dataReceiver, scheduledJob);
    }

    @Override
    public void deleteAllData() {
        log.info("Delete all data");
        dataReceiver.stopAndClean();
        scheduledJob.stop();
        List<String> allIndexes = ElasticsearchUtil.getAllArktourosIndexes();
        ElasticsearchUtil.truncateIndexes(allIndexes);
        dataReceiver.flushAndStart();
        scheduledJob.flushAndStart();
        log.info("Delete all data successfully");
    }

    @Override
    public void deleteAllLogs() {
        log.info("Delete all logs");
        dataReceiver.stopAndClean();
        scheduledJob.stop();
        List<String> allLogIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(LOG_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allLogIndexes);
        dataReceiver.flushAndStart();
        scheduledJob.flushAndStart();
        log.info("Delete all logs successfully");
    }

    @Override
    public void deleteAllSpans() {
        log.info("Delete all spans");
        dataReceiver.stopAndClean();
        scheduledJob.stop();
        List<String> allSpanIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(SPAN_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allSpanIndexes);
        dataReceiver.flushAndStart();
        scheduledJob.flushAndStart();
        log.info("Delete all spans successfully");
    }

    @Override
    public void deleteAllMetrics() {
        log.info("Delete all metrics");
        dataReceiver.stopAndClean();
        scheduledJob.stop();
        List<String> allMetricIndexes = ElasticsearchUtil.getAllArktourosIndexes().stream().filter(
                index -> index.startsWith(GAUGE_INDEX.getIndexName())||
                        index.startsWith(COUNTER_INDEX.getIndexName()) ||
                        index.startsWith(HISTOGRAM_INDEX.getIndexName()) ||
                        index.startsWith(SUMMARY_INDEX.getIndexName())
        ).toList();
        ElasticsearchUtil.truncateIndexes(allMetricIndexes);
        dataReceiver.flushAndStart();
        scheduledJob.flushAndStart();
        log.info("Delete all metrics successfully");
    }
}

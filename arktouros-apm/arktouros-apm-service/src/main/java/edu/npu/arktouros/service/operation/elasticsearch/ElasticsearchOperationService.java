package edu.npu.arktouros.service.operation.elasticsearch;

import java.util.List;

import com.jayway.jsonpath.internal.function.sequence.Index;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.queue.TraceQueueService;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;

/**
 * @author : [wangminan]
 * @description : Elasticsearch数据运维服务
 */
public class ElasticsearchOperationService extends DataOperationService {

    public ElasticsearchOperationService(DataReceiver dataReceiver, LogQueueService logQueueService,
                                         TraceQueueService traceQueueService, MetricsQueueService metricsQueueService) {
        super(dataReceiver, logQueueService, traceQueueService, metricsQueueService);
    }

    @Override
    public void deleteAllLogs() {
        List<String> allLogIndexes = ElasticsearchUtil.getAllActualIndexes().stream().filter(
                index -> index.contains("arktouros-log")
        ).toList();
    }

    @Override
    public void deleteAllSpans() {
    }

    @Override
    public void deleteAllMetrics() {

    }
}

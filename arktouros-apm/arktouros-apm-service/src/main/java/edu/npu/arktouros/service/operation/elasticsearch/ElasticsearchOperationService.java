package edu.npu.arktouros.service.operation.elasticsearch;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.queue.TraceQueueService;

/**
 * @author : [wangminan]
 * @description : Elasticsearch数据运维服务
 */
public class ElasticsearchOperationService extends DataOperationService {

    public ElasticsearchOperationService(DataReceiver dataReceiver, LogQueueService logQueueService,
                                         TraceQueueService traceQueueService, MetricsQueueService metricsQueueService) {
        super(dataReceiver, logQueueService, traceQueueService, metricsQueueService);
    }
}

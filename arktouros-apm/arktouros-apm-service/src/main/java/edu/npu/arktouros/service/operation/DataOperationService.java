package edu.npu.arktouros.service.operation;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.queue.TraceQueueService;

/**
 * @author : [wangminan]
 * @description : 数据运维服务
 */

public abstract class DataOperationService {

    private DataReceiver dataReceiver;
    private LogQueueService logQueueService;
    private TraceQueueService traceQueueService;
    private MetricsQueueService metricsQueueService;

    public DataOperationService(DataReceiver dataReceiver, LogQueueService logQueueService,
                                TraceQueueService traceQueueService,
                                MetricsQueueService metricsQueueService) {
        this.dataReceiver = dataReceiver;
        this.logQueueService = logQueueService;
        this.traceQueueService = traceQueueService;
        this.metricsQueueService = metricsQueueService;
    }
}

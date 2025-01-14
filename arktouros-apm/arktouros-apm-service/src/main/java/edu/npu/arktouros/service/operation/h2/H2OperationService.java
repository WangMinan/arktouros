package edu.npu.arktouros.service.operation.h2;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.queue.LogQueueService;
import edu.npu.arktouros.service.queue.MetricsQueueService;
import edu.npu.arktouros.service.queue.TraceQueueService;

/**
 * @author : [wangminan]
 * @description : H2数据运维服务
 */
public class H2OperationService extends DataOperationService {

    public H2OperationService(DataReceiver dataReceiver, LogQueueService logQueueService, TraceQueueService traceQueueService, MetricsQueueService metricsQueueService) {
        super(dataReceiver, logQueueService, traceQueueService, metricsQueueService);
    }

    @Override
    public void deleteAllLogs() {
    }

    @Override
    public void deleteAllSpans() {
    }

    @Override
    public void deleteAllMetrics() {

    }
}

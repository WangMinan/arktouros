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

    protected DataReceiver dataReceiver;

    public DataOperationService(DataReceiver dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    public abstract void deleteAllData();

    public abstract void deleteAllLogs();
    public abstract void deleteAllSpans();
    public abstract void deleteAllMetrics();
}

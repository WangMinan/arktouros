package edu.npu.arktouros.service.operation;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.scheduled.ScheduledJob;

/**
 * @author : [wangminan]
 * @description : 数据运维服务
 */

public abstract class DataOperationService {

    protected DataReceiver dataReceiver;
    protected ScheduledJob scheduledJob;

    public DataOperationService(DataReceiver dataReceiver, ScheduledJob scheduledJob) {
        this.dataReceiver = dataReceiver;
        this.scheduledJob = scheduledJob;
    }

    public abstract void deleteAllData();

    public abstract void deleteAllLogs();
    public abstract void deleteAllSpans();
    public abstract void deleteAllMetrics();
}

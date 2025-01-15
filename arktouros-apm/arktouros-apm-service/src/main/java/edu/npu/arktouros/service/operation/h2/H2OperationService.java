package edu.npu.arktouros.service.operation.h2;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.operation.DataOperationService;
import edu.npu.arktouros.service.scheduled.ScheduledJob;

/**
 * @author : [wangminan]
 * @description : H2数据运维服务
 */
public class H2OperationService extends DataOperationService {

    public H2OperationService(DataReceiver dataReceiver, ScheduledJob scheduledJob) {
        super(dataReceiver, scheduledJob);
    }

    @Override
    public void deleteAllData() {
        throw new UnsupportedOperationException("H2 does not support delete all data");
    }

    @Override
    public void deleteAllLogs() {
        throw new UnsupportedOperationException("H2 does not support delete all logs");
    }

    @Override
    public void deleteAllSpans() {
        throw new UnsupportedOperationException("H2 does not support delete all spans");
    }

    @Override
    public void deleteAllMetrics() {
        throw new UnsupportedOperationException("H2 does not support delete all metrics");

    }
}

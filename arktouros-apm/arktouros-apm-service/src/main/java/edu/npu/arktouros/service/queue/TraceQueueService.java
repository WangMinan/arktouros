package edu.npu.arktouros.service.queue;

import edu.npu.arktouros.model.queue.TraceQueueItem;
import edu.npu.arktouros.service.QueueService;

/**
 * @author : [wangminan]
 * @description : 链路追踪队列服务
 */
public class TraceQueueService implements QueueService<TraceQueueItem> {
    @Override
    public void put(TraceQueueItem traceQueueItem) {

    }

    @Override
    public void get(TraceQueueItem traceQueueItem) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public long size() {
        return 0;
    }
}

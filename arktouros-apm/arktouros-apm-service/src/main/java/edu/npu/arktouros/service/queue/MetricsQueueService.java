package edu.npu.arktouros.service.queue;

import edu.npu.arktouros.model.queue.MetricsQueueItem;
import edu.npu.arktouros.service.QueueService;

/**
 * @author : [wangminan]
 * @description : 数值队列服务
 */
public class MetricsQueueService implements QueueService<MetricsQueueItem> {
    @Override
    public void put(MetricsQueueItem metricsQueueItem) {

    }

    @Override
    public void get(MetricsQueueItem metricsQueueItem) {

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

package edu.npu.arktouros.service.otel.queue;

import edu.npu.arktouros.mapper.otel.queue.MetricsQueueMapper;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author : [wangminan]
 * @description : 数值队列服务
 */
@Service
public class MetricsQueueService implements QueueService<MetricsQueueItem> {

    @Resource
    private MetricsQueueMapper metricsQueueMapper;

    @Override
    public void put(MetricsQueueItem metricsQueueItem) {
        metricsQueueMapper.add(metricsQueueItem);
    }

    @Override
    public MetricsQueueItem get() {
        return metricsQueueMapper.getTop();
    }

    @Override
    public boolean isEmpty() {
        return metricsQueueMapper.isEmpty();
    }

    @Override
    public long size() {
        return metricsQueueMapper.getSize();
    }
}

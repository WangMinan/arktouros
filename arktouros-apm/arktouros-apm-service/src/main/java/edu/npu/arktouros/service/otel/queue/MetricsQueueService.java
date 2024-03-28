package edu.npu.arktouros.service.otel.queue;

import edu.npu.arktouros.mapper.otel.queue.MetricsQueueMapper;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : 数值队列服务
 */
@Service
public class MetricsQueueService implements QueueService<MetricsQueueItem> {

    @Resource
    private MetricsQueueMapper metricsQueueMapper;

    // 模仿LinkedBlockingQueue的实现
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(MetricsQueueItem metricsQueueItem) {
        metricsQueueMapper.add(metricsQueueItem);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public MetricsQueueItem get() {
        MetricsQueueItem item = metricsQueueMapper.getTop();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (item == null) {
                notEmpty.await();
                item = metricsQueueMapper.getTop();
            }
        } finally {
            lock.unlock();
        }
        return item;
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

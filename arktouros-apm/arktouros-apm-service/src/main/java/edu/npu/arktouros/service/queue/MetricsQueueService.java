package edu.npu.arktouros.service.queue;

import edu.npu.arktouros.mapper.otel.queue.MetricsQueueMapper;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : 数值队列服务
 */
@Service
@Slf4j
public class MetricsQueueService extends QueueService<MetricsQueueItem> {

    @Resource
    private MetricsQueueMapper queueMapper;

    public MetricsQueueService() {
        this.name = "MetricsQueueService";
    }

    // 模仿LinkedBlockingQueue的实现
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(MetricsQueueItem metricsQueueItem) {
        queueMapper.add(metricsQueueItem);
        final ReentrantLock finalLock = this.lock;
        finalLock.lock();
        try {
            notEmpty.signal();
        } finally {
            finalLock.unlock();
        }
    }

    @SneakyThrows
    public MetricsQueueItem getItem(boolean removeAtSameTime) {
        MetricsQueueItem item = queueMapper.getTop();
        final ReentrantLock finalLock = this.lock;
        finalLock.lock();
        try {
            while (item == null) {
                notEmpty.await();
                item = queueMapper.getTop();
            }
            if (removeAtSameTime) {
                queueMapper.removeTop();
            }
        } catch (InterruptedException e) {
            log.warn("Force traceQueueService shutting down");
            Thread.currentThread().interrupt();
        }  finally {
            finalLock.unlock();
        }
        return item;
    }

    @Override
    public MetricsQueueItem get(boolean removeAtSameTime) {
        return getItem(removeAtSameTime);
    }

    @Override
    public MetricsQueueItem get() {
        return getItem(true);
    }

    @Override
    public boolean isEmpty() {
        return queueMapper.isEmpty();
    }

    @Override
    public long size() {
        return queueMapper.getSize();
    }

    @Override
    public void waitTableReady() {
        queueMapper.prepareTable();
    }

    @Override
    void clear() {
        queueMapper.clear();
    }
}

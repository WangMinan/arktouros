package edu.npu.arktouros.service.otel.queue;


import edu.npu.arktouros.mapper.otel.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : log队列服务
 */
@Service
public class LogQueueService implements QueueService<LogQueueItem> {

    @Resource
    private LogQueueMapper queueMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(LogQueueItem logQueueItem) {
        queueMapper.add(logQueueItem);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @SneakyThrows
    public LogQueueItem get() {
        LogQueueItem item = queueMapper.getTop();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (item == null) {
                notEmpty.await();
                item = queueMapper.getTop();
            }
        } finally {
            lock.unlock();
        }
        return item;
    }

    @Override
    public boolean isEmpty() {
        return queueMapper.isEmpty();
    }

    @Override
    public long size() {
        return queueMapper.getSize();
    }
}

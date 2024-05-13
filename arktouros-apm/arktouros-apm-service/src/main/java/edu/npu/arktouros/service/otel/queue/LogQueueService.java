package edu.npu.arktouros.service.otel.queue;


import edu.npu.arktouros.mapper.otel.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : log队列服务
 */
@Service
@Slf4j
public class LogQueueService extends QueueService<LogQueueItem> {

    @Resource
    private LogQueueMapper queueMapper;

    public LogQueueService() {
        this.name = "LogQueueService";
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(LogQueueItem logQueueItem) {
        queueMapper.add(logQueueItem);
        final ReentrantLock finalLock = this.lock;
        finalLock.lock();
        try {
            notEmpty.signal();
        } finally {
            finalLock.unlock();
        }
    }

    @Override
    public LogQueueItem get() {
        return getItem(true);
    }

    @Override
    public LogQueueItem get(boolean removeAtSameTime) {
        return getItem(removeAtSameTime);
    }

    private LogQueueItem getItem(boolean removeAtSameTime) {
        LogQueueItem item;
        lock.lock();
        try {
            while ((item = queueMapper.getTop()) == null) {
                notEmpty.await();
            }
            if (removeAtSameTime) {
                queueMapper.removeTop();
            }
            return item;
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for item.");
        } finally {
            lock.unlock();
        }
        return null;
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
}

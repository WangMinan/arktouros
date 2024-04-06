package edu.npu.arktouros.service.otel.queue;

import edu.npu.arktouros.mapper.otel.queue.TraceQueueMapper;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : 链路追踪队列服务
 */
@Service
@Slf4j
public class TraceQueueService extends QueueService<TraceQueueItem> {

    @Resource
    private TraceQueueMapper queueMapper;

    public TraceQueueService() {
        this.name = "TraceQueueService";
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(TraceQueueItem traceQueueItem) {
        queueMapper.add(traceQueueItem);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TraceQueueItem get() {
        return getItem(true);
    }

    @Override
    public TraceQueueItem get(boolean removeAtSameTime) {
        return getItem(removeAtSameTime);
    }

    @SneakyThrows
    public TraceQueueItem getItem(boolean removeAtSameTime) {
        TraceQueueItem item = queueMapper.getTop();
        final ReentrantLock lock = this.lock;
        lock.lock();
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

    @Override
    public void waitTableReady() {
        queueMapper.prepareTable();
    }
}

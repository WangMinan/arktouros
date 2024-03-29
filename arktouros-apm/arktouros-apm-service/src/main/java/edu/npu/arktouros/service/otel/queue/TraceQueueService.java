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
public class TraceQueueService implements QueueService<TraceQueueItem> {

    @Resource
    private TraceQueueMapper traceQueueMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(TraceQueueItem traceQueueItem) {
        traceQueueMapper.add(traceQueueItem);
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
    public TraceQueueItem get() {
        TraceQueueItem item = traceQueueMapper.getTop();
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (item == null) {
                notEmpty.await();
                item = traceQueueMapper.getTop();
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
        return traceQueueMapper.isEmpty();
    }

    @Override
    public long size() {
        return traceQueueMapper.getSize();
    }
}

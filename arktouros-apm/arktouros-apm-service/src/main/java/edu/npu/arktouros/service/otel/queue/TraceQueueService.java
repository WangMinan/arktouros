package edu.npu.arktouros.service.otel.queue;

import edu.npu.arktouros.mapper.otel.queue.TraceQueueMapper;
import edu.npu.arktouros.model.queue.TraceQueueItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author : [wangminan]
 * @description : 链路追踪队列服务
 */
@Service
public class TraceQueueService implements QueueService<TraceQueueItem> {

    @Resource
    private TraceQueueMapper traceQueueMapper;

    @Override
    public void put(TraceQueueItem traceQueueItem) {
        traceQueueMapper.add(traceQueueItem);
    }

    @Override
    public TraceQueueItem get() {
        return traceQueueMapper.getTop();
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

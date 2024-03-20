package edu.npu.arktouros.service.otel.queue;


import edu.npu.arktouros.mapper.otel.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author : [wangminan]
 * @description : log队列服务
 */
@Service
public class LogQueueService implements QueueService<LogQueueItem> {

    @Resource
    private LogQueueMapper queueMapper;

    @Override
    public void put(LogQueueItem logQueueItem) {
        queueMapper.add(logQueueItem);
    }

    @Override
    public LogQueueItem get() {
        return queueMapper.getTop();
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

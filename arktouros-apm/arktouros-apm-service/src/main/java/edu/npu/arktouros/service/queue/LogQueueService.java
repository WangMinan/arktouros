package edu.npu.arktouros.service.queue;


import edu.npu.arktouros.mapper.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import edu.npu.arktouros.service.QueueService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author : [wangminan]
 * @description : log队列服务
 */
@Service
public class LogQueueService implements QueueService<LogQueueItem> {

    @Resource
    private LogQueueMapper mapper;

    @Override
    public void put(LogQueueItem logQueueItem) {
        mapper.add(logQueueItem);
    }

    @Override
    public void get(LogQueueItem logQueueItem) {

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

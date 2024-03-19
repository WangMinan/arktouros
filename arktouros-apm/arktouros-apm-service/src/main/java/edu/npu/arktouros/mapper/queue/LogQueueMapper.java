package edu.npu.arktouros.mapper.queue;

import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author : [wangminan]
 * @description : apm_log_queue表对应的mapper
 */
@Repository
public class LogQueueMapper {
    @Resource
    private JdbcTemplate jdbcTemplate;

    public void add(LogQueueItem logQueueItem) {

    }
}

package edu.npu.arktouros.model.queue;

import lombok.Data;

/**
 * @author : [wangminan]
 * @description : 日志队列item
 */
@Data
public class LogQueueItem {
    private Long id;
    private String data;
}

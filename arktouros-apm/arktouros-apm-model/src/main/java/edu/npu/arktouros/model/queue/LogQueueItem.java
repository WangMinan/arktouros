package edu.npu.arktouros.model.queue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : [wangminan]
 * @description : 日志队列item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQueueItem {
    private Long id;
    private String data;
}

package edu.npu.arktouros.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author : [wangminan]
 * @description : 测试日志队列缓存
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogQueueCacheTest {

    @Test
    @Timeout(10)
    void testPutAndGet() throws Exception {
        LogQueueCache cache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        cache.put("123");
        Assertions.assertFalse(cache.isEmpty());
        Assertions.assertEquals("123", cache.get());
        Assertions.assertTrue(cache.isEmpty());

        LogQueueCache cacheException1 = new LogQueueCache();
        ArrayBlockingQueue<String> queue = Mockito.mock(ArrayBlockingQueue.class);
        LogQueueCache.queue = queue;
        Mockito.doThrow(new InterruptedException()).when(queue).take();
        Assertions.assertThrows(RuntimeException.class, cacheException1::get);

        LogQueueCache cacheException2 = new LogQueueCache();
        ArrayBlockingQueue<String> queue2 = Mockito.mock(ArrayBlockingQueue.class);
        LogQueueCache.queue = queue2;
        Mockito.doThrow(new InterruptedException()).when(queue2).put("123");
        Assertions.assertThrows(RuntimeException.class, () -> cacheException2.put("123"));
    }
}

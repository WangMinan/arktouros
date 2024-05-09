package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.LogQueueCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : [wangminan]
 * @description : {@link OtlpLogPreHandler}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class OtlpLogPreHandlerTest {

    // 线程池
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(1);

    @Test
    void testRunError() {
        LogQueueCache inputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        LogQueueCache outputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        AbstractPreHandler handler =
                new OtlpLogPreHandler.Factory().createPreHandler(inputCache, outputCache);
        inputCache.put("abc");
        // 不知道为什么mvn test的时候这个位置会变成nullPointerException 本地跑没问题的
        Assertions.assertThrows(Exception.class, handler::run);
        handler.interrupt();
    }

    @Test
    void testRun() {
        LogQueueCache inputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        LogQueueCache outputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        AbstractPreHandler handler =
                new OtlpLogPreHandler.Factory().createPreHandler(inputCache, outputCache);
        inputCache.put("   {}");
        executorService.submit(handler);
        // 等待
        try {
            Thread.sleep(1000);
            handler.interrupt();
        } catch (InterruptedException e) {
            log.error("Thread sleep error", e);
        }
        // 不知道为什么mvn test的时候这个位置会变成nullPointerException 本地跑没问题的
        Assertions.assertEquals("{}", outputCache.get());
        executorService.shutdown();
    }
}

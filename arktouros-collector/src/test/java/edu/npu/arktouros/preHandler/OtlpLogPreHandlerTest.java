package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

    private LogQueueCache inputCache;
    private LogQueueCache outputCache;
    private OtlpLogPreHandler handler;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void init() {
        inputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        outputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        handler = (OtlpLogPreHandler) new OtlpLogPreHandler.Factory().createPreHandler(inputCache, outputCache);
    }

    @Test
    void testRunError() {
        inputCache.put("abc");
        // 不知道为什么mvn test的时候这个位置会变成nullPointerException 本地跑没问题的
        handler.start();
        handler.interrupt();
        Assertions.assertTrue(outputCache.isEmpty());
    }

    @Test
    void testRun() {
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

package edu.npu.arktouros.preHandler;

import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : {@link JsonLogPreHandler}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsonLogPreHandlerTest {

    // 线程池
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(1);

    private LogQueueCache inputCache;
    private LogQueueCache outputCache;
    private JsonLogPreHandler handler;

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @BeforeEach
    void init() {
        inputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        outputCache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        handler = (JsonLogPreHandler) new JsonLogPreHandler.Factory().createPreHandler(inputCache, outputCache);
    }

    @Test
    @Timeout(value = 30000, unit = TimeUnit.MILLISECONDS) // 默认单位秒
    void testRun() {
        log.info("testRun");
        executorService.submit(handler);
        for (int i = 0; i < 100; i++) {
            inputCache.put("   {}");
        }
        // 不知道为什么mvn test的时候这个位置会变成nullPointerException 本地跑没问题的
        Assertions.assertNotNull(outputCache.get());
        executorService.shutdown();
    }

    @Test
    @Timeout(30)
    void testRunError() {
        log.info("testRunError");
        inputCache.put("abc");
        // 不知道为什么mvn test的时候这个位置会变成nullPointerException 本地跑没问题的
        handler.start();
        handler.interrupt();
        Assertions.assertFalse(outputCache.isEmpty());
    }
}

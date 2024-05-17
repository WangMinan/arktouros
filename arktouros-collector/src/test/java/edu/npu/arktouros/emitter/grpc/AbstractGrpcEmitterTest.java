package edu.npu.arktouros.emitter.grpc;

import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractGrpcEmitterTest {

    @BeforeAll
    static void initProperties() {
        PropertiesProvider.init();
    }

    @Test
    @Timeout(30)
    void testKeepAlive() throws
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("testKeepAlive");
        LogQueueCache cache = (LogQueueCache) new LogQueueCache.Factory().createCache();
        AbstractGrpcEmitter emitter = new AbstractGrpcEmitter(cache);
        Method keepAliveCheck =
                AbstractGrpcEmitter.class
                        .getDeclaredMethod("startKeepAliveCheck", CountDownLatch.class);
        keepAliveCheck.setAccessible(true);
        keepAliveCheck.invoke(emitter, new CountDownLatch(1));
    }
}

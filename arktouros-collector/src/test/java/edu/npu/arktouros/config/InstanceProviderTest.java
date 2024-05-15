package edu.npu.arktouros.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author : [wangminan]
 * @description : {@link InstanceProvider}
 */
@ExtendWith({MockitoExtension.class})
// 要加这个配置 不然对any的类型推断有很严格的限制
@MockitoSettings(strictness = Strictness.LENIENT)
class InstanceProviderTest {

    @BeforeAll
    static void setUp() {
        PropertiesProvider.init();
    }

    @Test
    void testConstructor() throws NoSuchMethodException {
        Constructor<InstanceProvider> constructor = InstanceProvider.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Assertions.assertThrows(Exception.class, constructor::newInstance);
    }

    @Test
    void testGetReceiverError() {
        InstanceProvider.receiverClassName = "test";
        Assertions.assertThrows(IllegalArgumentException.class, InstanceProvider::getReceiver);
    }

    @Test
    void testGetPreHandlerError() {
        InstanceProvider.preHandlerClassName = "test";
        Assertions.assertThrows(IllegalArgumentException.class, InstanceProvider::getPreHandler);
    }

    @Test
    void testGetEmitter() {
        InstanceProvider.emitterClassName = "OtelGrpcEmitter";
        Assertions.assertNotNull(InstanceProvider.getEmitter());
        InstanceProvider.emitterClassName = "ArktourosGrpcEmitter";
        Assertions.assertNotNull(InstanceProvider.getEmitter());
        InstanceProvider.emitterClassName = "test";
        Assertions.assertThrows(IllegalArgumentException.class, InstanceProvider::getEmitter);
    }

    @Test
    void getNewCacheError() throws NoSuchMethodException {
        InstanceProvider.cacheClassName = "test";
        Method getNewCache = InstanceProvider.class.getDeclaredMethod("getNewCache");
        getNewCache.setAccessible(true);
        Assertions.assertThrows(Exception.class,
                () -> getNewCache.invoke(null));
    }
}

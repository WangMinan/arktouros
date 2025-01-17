package edu.npu.arktouros.util.elasticsearch.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.model.config.PropertiesProvider;
import edu.npu.arktouros.model.exception.ArktourosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Constructor;
import java.time.Duration;

/**
 * @author : [wangminan]
 * @description : {@link ElasticsearchClientPool}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class ElasticsearchClientPoolTest {

    private static ElasticsearchClientPoolFactory factory;

    private static GenericObjectPool<ElasticsearchClient> pool;

    @BeforeAll
    static void beforeAll() {
        PropertiesProvider.init();
        factory = Mockito.mock(
                ElasticsearchClientPoolFactory.class);
        GenericObjectPoolConfig<ElasticsearchClient> poolConfig =
                new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(3);
        poolConfig.setMinIdle(3);
        poolConfig.setMaxWait(Duration.ofMillis(3));
        poolConfig.setJmxEnabled(false);
        // 当对象池耗尽时，是否等待获取对象
        poolConfig.setBlockWhenExhausted(true);
        // 创建对象时是否进行对象有效性检查
        poolConfig.setTestOnCreate(true);
        // 借出对象时是否进行对象有效性检查
        poolConfig.setTestOnBorrow(true);
        // 归还对象时是否进行对象有效性检查
        poolConfig.setTestOnReturn(true);
        // 空闲时是否进行对象有效性检查
        poolConfig.setTestWhileIdle(true);
        Mockito.when(factory.getPoolConfig()).thenReturn(poolConfig);
        pool = new GenericObjectPool<>(factory,
                factory.getPoolConfig());
        ElasticsearchClientPool.factory = factory;
        ElasticsearchClientPool.pool = pool;
    }

    @Test
    void testConstructor() throws NoSuchMethodException {
        Constructor<ElasticsearchClientPool> poolConstructor =
                ElasticsearchClientPool.class.getDeclaredConstructor();
        poolConstructor.setAccessible(true);
        Assertions.assertThrows(Exception.class, poolConstructor::newInstance);
    }

    @Test
    void testGetInstance() {
        Assertions.assertEquals(pool, ElasticsearchClientPool.getInstance());
    }

    @Test
    void testGetClient() throws Exception {
        Mockito.when(factory.makeObject()).thenReturn(Mockito.mock(PooledObject.class));
        Assertions.assertThrows(ArktourosException.class, ElasticsearchClientPool::getClient);
    }

    @Test
    void testReturnClient() {
        ElasticsearchClient client = Mockito.mock(ElasticsearchClient.class);
        Assertions.assertThrows(IllegalStateException.class, () -> ElasticsearchClientPool.returnClient(client));
    }
}

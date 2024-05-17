package edu.npu.arktouros.util.elasticsearch.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author : [wangminan]
 * @description : {@link ElasticsearchClientPoolFactory}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class ElasticsearchClientPoolFactoryTest {

    private MockedStatic<PropertiesProvider> providerMockedStatic;

    private final String ca = """
            -----BEGIN CERTIFICATE-----
            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
            -----END CERTIFICATE-----
            """;

    @BeforeEach
    void setUp() {
        providerMockedStatic = Mockito.mockStatic(PropertiesProvider.class);
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.serverUrl"))
                .thenReturn("127.0.0.1");
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.username"))
                .thenReturn("elastic");
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.password"))
                .thenReturn("elastic");
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.pool.maxTotal"))
                .thenReturn("10");
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.pool.maxWait"))
                .thenReturn("1000");
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.pool.minIdle"))
                .thenReturn("5");
    }

    @AfterEach
    void tearDown() {
        providerMockedStatic.close();
    }

    @Test
    void testMakeObject() throws Exception {
        ElasticsearchClientPoolFactory factory = new ElasticsearchClientPoolFactory();
        Assertions.assertNotNull(factory.makeObject());
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.ca"))
                .thenReturn(ca);
        factory = new ElasticsearchClientPoolFactory();
        Assertions.assertNotNull(factory.makeObject());
    }

    @Test
    void testCreateSSLContext() throws Exception {
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.ca"))
                .thenReturn(ca);
        ElasticsearchClientPoolFactory factory = new ElasticsearchClientPoolFactory();
        Assertions.assertNotNull(factory.createSSLContext());
        providerMockedStatic.when(() -> PropertiesProvider.getProperty("elasticsearch.ca"))
                .thenReturn("abc");
        factory = new ElasticsearchClientPoolFactory();
        Assertions.assertThrows(IllegalArgumentException.class, factory::createSSLContext);
    }

    @Test
    void testPassivateObject() {
        ElasticsearchClientPoolFactory factory = new ElasticsearchClientPoolFactory();
        PooledObject<ElasticsearchClient> pooledObject = Mockito.mock(PooledObject.class);
        Assertions.assertDoesNotThrow(() -> factory.passivateObject(pooledObject));
    }

    @Test
    void testActivateObject() {
        ElasticsearchClientPoolFactory factory = new ElasticsearchClientPoolFactory();
        PooledObject<ElasticsearchClient> pooledObject = Mockito.mock(PooledObject.class);
        Assertions.assertDoesNotThrow(() -> factory.activateObject(pooledObject));
    }

    @Test
    void testDestroyObject() {
        ElasticsearchClientPoolFactory factory = new ElasticsearchClientPoolFactory();
        PooledObject<ElasticsearchClient> pooledObject = Mockito.mock(PooledObject.class);
        ElasticsearchClient elasticsearchClient = Mockito.mock(ElasticsearchClient.class);
        Mockito.when(pooledObject.getObject()).thenReturn(elasticsearchClient);
        Mockito.when(elasticsearchClient.shutdown()).thenThrow(RuntimeException.class);
        Assertions.assertThrows(RuntimeException.class, () -> factory.destroyObject(pooledObject));
    }
}

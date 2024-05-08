package edu.npu.arktouros.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author : [wangminan]
 * @description : {@link PropertiesProvider}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PropertiesProviderTest {

    @Test
    void testGetProperty() {
        PropertiesProvider.init();
        Assertions.assertEquals(PropertiesProvider.getProperty("instance.cache"), "LogQueueCache");
        Assertions.assertEquals(PropertiesProvider.getProperty("server.host", "host"),
                "host");
    }
}

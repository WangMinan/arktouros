package edu.npu.arktouros.commons;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author : [wangminan]
 * @description : {@link JacksonConfig}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class JacksonConfigTest {

    @Test
    @Timeout(30)
    void testJacksonConfig() {
        JacksonConfig instance = JacksonConfig.instance;
        Assertions.assertNotNull(instance.getObjectMapper());
    }
}

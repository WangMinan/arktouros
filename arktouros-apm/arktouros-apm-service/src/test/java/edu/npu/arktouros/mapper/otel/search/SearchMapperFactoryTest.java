package edu.npu.arktouros.mapper.otel.search;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author : [wangminan]
 * @description : {@link SearchMapperFactory}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class SearchMapperFactoryTest {

    @Resource
    private SearchMapperFactory factory;

    @Test
    void testGetObject() {
        ReflectionTestUtils.setField(factory, "activeSearchMapper", "h2");
        SearchMapper searchMapper = factory.getObject();
        Assertions.assertNotNull(searchMapper);
    }

    @Test
    void testGetObjectError() {
        ReflectionTestUtils.setField(factory, "activeSearchMapper", "h3");
        Assertions.assertThrows(IllegalArgumentException.class, () -> factory.getObject());
    }
}

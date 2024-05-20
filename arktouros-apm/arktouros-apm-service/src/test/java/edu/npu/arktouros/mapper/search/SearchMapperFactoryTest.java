package edu.npu.arktouros.mapper.otel.search;

import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.mapper.search.SearchMapperFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
// 这玩意只能单独拿出来测 不然会影响后面的MapperTest 这个环境变量改不回来
@Disabled
class SearchMapperFactoryTest {

    @Resource
    private SearchMapperFactory factory;

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(factory, "activeSearchMapper", "elasticsearch");
    }

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

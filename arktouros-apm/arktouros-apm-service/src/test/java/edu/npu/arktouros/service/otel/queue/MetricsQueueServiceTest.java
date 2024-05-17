package edu.npu.arktouros.service.otel.queue;

import edu.npu.arktouros.mapper.otel.queue.MetricsQueueMapper;
import edu.npu.arktouros.model.queue.MetricsQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author : [wangminan]
 * @description : {@link MetricsQueueService}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
public class MetricsQueueServiceTest {
    @MockBean
    private MetricsQueueMapper queueMapper;

    @Resource
    private MetricsQueueService queueService;

    @Test
    void testPut() {
        Assertions.assertEquals("MetricsQueueService", queueService.getName());
        MetricsQueueItem item = MetricsQueueItem.builder().build();
        Assertions.assertDoesNotThrow(() -> queueService.put(item));
    }

    @Test
    void testGetItem() {
        Mockito.when(queueMapper.getTop()).thenReturn(MetricsQueueItem.builder().build());
        Assertions.assertDoesNotThrow(() -> queueService.get(false));
        Assertions.assertDoesNotThrow(() -> queueService.get(true));
    }

    @Test
    void testIsEmpty() {
        Mockito.when(queueMapper.isEmpty()).thenReturn(true);
        Assertions.assertTrue(queueService.isEmpty());
    }

    @Test
    void testGetSize() {
        Mockito.when(queueMapper.getSize()).thenReturn(1L);
        Assertions.assertEquals(1, queueService.size());
    }
}

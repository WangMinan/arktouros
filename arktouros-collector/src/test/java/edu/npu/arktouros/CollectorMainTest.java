package edu.npu.arktouros;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CountDownLatch;

/**
 * @author : [wangminan]
 * @description : 测试主启动类
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CollectorMainTest {

    @Test
    void testMain() throws Exception {
        CountDownLatch latch = Mockito.mock(CountDownLatch.class);
        Mockito.doThrow(new InterruptedException()).when(latch).await();
        CollectorMain.runningLatch = latch;
        Assertions.assertThrows(InterruptedException.class,
                () -> CollectorMain.main(new String[]{}));
    }
}

package edu.npu.arktouros.preHandler;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author : [wangminan]
 * @description : countdownLatch测试
 */
@Slf4j
public class CountDownLatchTest {

    @Test
    void countDownLatchTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                log.info("Make main thread awake success.");
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        log.info("I am going to wait for a while.");
        latch.await();
        log.info("Main thread is awake.");
    }
}

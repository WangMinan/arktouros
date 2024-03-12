package edu.npu;

import edu.npu.config.InstanceProvider;
import edu.npu.properties.PropertiesProvider;
import edu.npu.receiver.AbstractReceiver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : [wangminan]
 * @description : [Collector主程序 启动类]
 */
@Slf4j
public class CollectorMain {

    private static final CountDownLatch runningLatch = new CountDownLatch(1);

    // 线程池 一个线程给receiver 一个线程给emitter
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws InterruptedException {
        PropertiesProvider.init();
        InstanceProvider.init();

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    executorService.shutdown();
                    log.info("Collector shutdown");
                })
        );
        AbstractReceiver receiver = InstanceProvider.getReceiver();
        // 启动receiver
        executorService.submit(receiver);

        runningLatch.await();
    }
}

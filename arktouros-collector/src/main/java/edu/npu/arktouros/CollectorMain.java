package edu.npu.arktouros;

import edu.npu.arktouros.config.InstanceProvider;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.emit.AbstractEmitter;
import edu.npu.arktouros.emit.grpc.GrpcEmitter;
import edu.npu.arktouros.preHandler.AbstractPreHandler;
import edu.npu.arktouros.receiver.AbstractReceiver;
import lombok.extern.slf4j.Slf4j;

import java.util.Timer;
import java.util.TimerTask;
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

    // 线程池 一个线程给receiver 一个线程给analyzer 一个线程给emitter
    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(3);

    public static void main(String[] args) throws InterruptedException {
        PropertiesProvider.init();
        InstanceProvider.init();

        log.info("Starting emitter.");
        AbstractEmitter emitter = InstanceProvider.getEmitter();
        emitter.setUncaughtExceptionHandler((t, e) -> {
            log.error("Emitter error", e);
            // 结束程序
            System.exit(1);
        });
        // 启动emitter
        executorService.submit(emitter);

        log.info("Starting preHandler.");
        AbstractPreHandler preHandler = InstanceProvider.getPreHandler();
        preHandler.setUncaughtExceptionHandler((t, e) -> {
            log.error("PreHandler error", e);
            // 结束程序
            System.exit(1);
        });
        // 启动preHandler
        executorService.submit(preHandler);

        log.info("Starting receiver.");
        AbstractReceiver receiver = InstanceProvider.getReceiver();
        receiver.setUncaughtExceptionHandler((t, e) -> {
            log.error("Receiver error", e);
            // 结束程序
            System.exit(1);
        });
        // 启动receiver
        executorService.submit(receiver);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    // 先暂停receiver
                    receiver.interrupt();

                    // 发送完emitter的cache中的所有数据
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (emitter.getInputCache().isEmpty()) {
                                timer.cancel();
                            }
                        }
                    }, 0, 1000);

                    // 停止发送
                    if (emitter instanceof GrpcEmitter) {
                        ((GrpcEmitter) emitter).getChannel().shutdown();
                    }
                    executorService.shutdown();
                    log.info("Collector shutdown");
                })
        );

        // 阻塞主线程
        runningLatch.await();
    }
}

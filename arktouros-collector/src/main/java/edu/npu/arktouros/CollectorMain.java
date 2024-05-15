package edu.npu.arktouros;

import edu.npu.arktouros.config.InstanceProvider;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.emitter.AbstractEmitter;
import edu.npu.arktouros.emitter.grpc.otel.OtelGrpcEmitter;
import edu.npu.arktouros.preHandler.AbstractPreHandler;
import edu.npu.arktouros.receiver.AbstractReceiver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.ArrayList;
import java.util.List;
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

    protected static CountDownLatch runningLatch = new CountDownLatch(1);
    public static final int preHandlerNum;
    private static final int emitterNum;
    // 线程池 receiver单线程
    private static final ExecutorService receiverThreadPool;
    private static final ExecutorService preHandlerThreadPool;
    private static final ExecutorService emitterThreadPool;

    static {
        PropertiesProvider.init();
        InstanceProvider.init();
        preHandlerNum = Integer.parseInt(
                PropertiesProvider.getProperty("instance.number.preHandler", "3"));
        emitterNum = Integer.parseInt(
                PropertiesProvider.getProperty("instance.number.emitter", "3"));
        receiverThreadPool = Executors.newSingleThreadExecutor(
                new BasicThreadFactory.Builder()
                        .namingPattern("receiver-%d").build());
        preHandlerThreadPool = Executors.newFixedThreadPool(preHandlerNum,
                new BasicThreadFactory.Builder()
                        .namingPattern("preHandler-%d").build());
        emitterThreadPool =
                Executors.newFixedThreadPool(emitterNum,
                        new BasicThreadFactory.Builder()
                                .namingPattern("emitter-%d").build());
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("Starting emitter.");
        List<AbstractEmitter> emitters = startEmitters();

        log.info("Starting preHandler.");
        startPreHandlers();

        log.info("Starting receiver.");
        AbstractReceiver receiver = startReceiver();

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> getShutdownThread(receiver, emitters))
        );

        // 阻塞主线程
        runningLatch.await();
    }

    private static AbstractReceiver startReceiver() {
        AbstractReceiver receiver = InstanceProvider.getReceiver();
        receiver.setUncaughtExceptionHandler((t, e) -> {
            log.error("Receiver error", e);
            // 结束程序
            System.exit(1);
        });
        // 启动receiver
        receiverThreadPool.submit(receiver);
        return receiver;
    }

    private static void startPreHandlers() {
        for (int i = 0; i < preHandlerNum; i++) {
            AbstractPreHandler preHandler = InstanceProvider.getPreHandler();
            preHandler.setUncaughtExceptionHandler((t, e) -> {
                log.error("PreHandler error", e);
                // 结束程序
                System.exit(1);
            });
            // 启动preHandler
            preHandlerThreadPool.submit(preHandler);
        }
    }

    private static void getShutdownThread(AbstractReceiver receiver, List<AbstractEmitter> emitters) {
        // 先暂停receiver
        receiver.interrupt();

        // 发送完emitter的cache中的所有数据
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean canShutdown = true;
                for (AbstractEmitter emitter : emitters) {
                    if (!emitter.getInputCache().isEmpty()) {
                        canShutdown = false;
                        break;
                    }
                }
                if (canShutdown) {
                    timer.cancel();
                }
            }
        }, 0, 1000);

        // 停止发送
        for (AbstractEmitter emitter : emitters) {
            if (emitter instanceof OtelGrpcEmitter) {
                ((OtelGrpcEmitter) emitter).getChannel().shutdown();
            }
        }
        receiverThreadPool.shutdown();
        preHandlerThreadPool.shutdown();
        emitterThreadPool.shutdown();
        log.info("Collector shutdown");
    }

    private static List<AbstractEmitter> startEmitters() {
        List<AbstractEmitter> emitters = new ArrayList<>();
        for (int i = 0; i < emitterNum; i++) {
            AbstractEmitter emitter = InstanceProvider.getEmitter();
            emitter.setUncaughtExceptionHandler((t, e) -> {
                log.error("Emitter error", e);
                // 结束程序
                System.exit(1);
            });
            emitters.add(emitter);
            // 启动emitter
            emitterThreadPool.submit(emitter);
        }
        return emitters;
    }
}

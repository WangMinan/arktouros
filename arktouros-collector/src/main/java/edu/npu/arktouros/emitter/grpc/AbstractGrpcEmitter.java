package edu.npu.arktouros.emitter.grpc;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.emitter.AbstractEmitter;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : [wangminan]
 * @description : 抽象Grpc接收器
 */
@Slf4j
public class AbstractGrpcEmitter extends AbstractEmitter {
    @Getter
    protected final ManagedChannel channel;

    // 一个探活线程
    protected final ScheduledThreadPoolExecutor keepAliveThreadPool =
            // 定时线程池
            new ScheduledThreadPoolExecutor(1,
                    new BasicThreadFactory.Builder()
                            .namingPattern("Keep-alive-check-%d").build());
    protected final AtomicInteger connectRetryTimes = new AtomicInteger(0);

    protected AbstractGrpcEmitter(AbstractCache inputCache) {
        super(inputCache);
        String HOST = PropertiesProvider
                .getProperty("emitter.grpc.host", "127.0.0.1");
        if (StringUtils.isEmpty(HOST) ||
                PropertiesProvider.getProperty("emitter.grpc.port") == null) {
            throw new IllegalArgumentException("Invalid host or port for grpc emitter");
        }
        int PORT = Integer.parseInt(PropertiesProvider.getProperty("emitter.grpc.port"));
        channel = ManagedChannelBuilder.forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        if (Boolean.parseBoolean(
                PropertiesProvider.getProperty("emitter.grpc.keepAlive.enabled",
                        "true")
        )) {
            CountDownLatch waitForFirstConnectLatch = new CountDownLatch(1);
            startKeepAliveCheck(waitForFirstConnectLatch);
            try {
                waitForFirstConnectLatch.await();
            } catch (InterruptedException e) {
                log.info("Interrupted when waiting for the first connection to apm.");
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private void startKeepAliveCheck(CountDownLatch waitForFirstConnectLatch) {
        long delay = Long.parseLong(
                PropertiesProvider.getProperty("emitter.grpc.keepAlive.delay",
                        "5"));
        Thread checkConnectThread = new Thread(() -> {
            try {
                if (waitForFirstConnectLatch.getCount() == 1) {
                    log.info("Waiting for the first connection to apm. Will take delay:{} seconds to establish the connection.",
                            delay);
                }
                ConnectivityState state = channel.getState(true);
                if (state.equals(ConnectivityState.READY) &&
                        waitForFirstConnectLatch.getCount() == 1
                ) {
                    connectRetryTimes.getAndSet(0);
                    log.info("Grpc emitter successfully connected to apm.");
                    waitForFirstConnectLatch.countDown();
                } else if (
                        state.equals(ConnectivityState.TRANSIENT_FAILURE) ||
                                state.equals(ConnectivityState.IDLE)
                ) {
                    int retryTimes = connectRetryTimes.getAndIncrement();
                    int maxRetryTimes = Integer.parseInt(
                            PropertiesProvider.getProperty(
                                    "emitter.grpc.keepAlive.maxRetryTimes",
                                    "3"));
                    if (retryTimes > maxRetryTimes) {
                        log.error("Failed to connect to apm after {} times, exit.",
                                maxRetryTimes);
                        throw new StatusRuntimeException(io.grpc.Status.UNAVAILABLE);
                    }
                } else if (state.equals(ConnectivityState.SHUTDOWN)) {
                    log.error("Grpc emitter has been shutdown, exit.");
                    throw new StatusRuntimeException(io.grpc.Status.UNAVAILABLE);
                }
            } catch (StatusRuntimeException e) {
                log.error("Failed to connect to apm", e);
                channel.shutdown();
                System.exit(1);
            }
        });
        log.info("Start grpc keep-alive check. Delay :{}s", delay);
        keepAliveThreadPool.scheduleWithFixedDelay(checkConnectThread, 0,
                delay, TimeUnit.SECONDS);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        channel.shutdown();
        keepAliveThreadPool.shutdown();
    }
}

package edu.npu.arktouros.emit.grpc;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.commons.ProtoBufJsonUtils;
import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.emit.AbstractEmitter;
import edu.npu.arktouros.emit.EmitterFactory;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpc;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.metrics.v1.MetricsData;
import io.opentelemetry.proto.trace.v1.TracesData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : [wangminan]
 * @description : [grpc发射器]
 */
@Slf4j
public class GrpcEmitter extends AbstractEmitter {

    @Getter
    private final ManagedChannel channel;
    private final LogsServiceGrpc.LogsServiceBlockingStub logsServiceBlockingStub;
    private final MetricsServiceGrpc.MetricsServiceBlockingStub metricsServiceBlockingStub;
    private final TraceServiceGrpc.TraceServiceBlockingStub traceServiceBlockingStub;

    // 一个探活线程
    private final ScheduledThreadPoolExecutor keepAliveThreadPool =
            new ScheduledThreadPoolExecutor(1);
    private final AtomicInteger connectRetryTimes = new AtomicInteger(0);

    public GrpcEmitter(AbstractCache inputCache) {
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
                throw new RuntimeException(e);
            }
        }

        logsServiceBlockingStub = LogsServiceGrpc.newBlockingStub(channel);
        metricsServiceBlockingStub = MetricsServiceGrpc.newBlockingStub(channel);
        traceServiceBlockingStub = TraceServiceGrpc.newBlockingStub(channel);
    }

    private void startKeepAliveCheck(CountDownLatch waitForFirstConnectLatch) {
        Thread checkConnectThread = new Thread(() -> {
            try {
                if (waitForFirstConnectLatch.getCount() == 1) {
                    log.info("Waiting for the first connection to apm.");
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
        long delay = Long.parseLong(
                PropertiesProvider.getProperty("emitter.grpc.keepAlive.delay",
                        "5"));
        log.info("Start grpc keep-alive check. Delay :{}s", delay);
        keepAliveThreadPool.scheduleWithFixedDelay(checkConnectThread, 0,
                delay, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        // 我们通过拿到的json串的前缀来判断这玩意是metrics logs还是trace
        while (true) {
            String inputJson = inputCache.get().trim();
            try {
                if (inputJson.startsWith("{\"resourceSpans\":")) {
                    handleTrace(inputJson);
                } else if (inputJson.startsWith("{\"resourceMetrics\":")) {
                    handleMetrics(inputJson);
                } else if (inputJson.startsWith("{\"resourceLogs\":")) {
                    handleLogs(inputJson);
                } else {
                    log.warn("Invalid input for json: {}", inputJson);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleLogs(String inputJson) throws IOException {
        LogsData.Builder builder = LogsData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        LogsData logsData = builder.build();
        log.info("Sending logs data to apm");
        ExportLogsServiceRequest request =
                ExportLogsServiceRequest
                        .newBuilder()
                        .addAllResourceLogs(logsData.getResourceLogsList())
                        .build();
        ExportLogsServiceResponse response = logsServiceBlockingStub.export(request);
        if (response.hasPartialSuccess()) {
            log.info("Send logs data to apm complete");
        } else {
            log.error("Failed to send logs data to apm, response:{}", response);
        }
    }

    private void handleMetrics(String inputJson) throws IOException {
        MetricsData.Builder builder = MetricsData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        MetricsData metricsData = builder.build();
        log.info("Sending metrics data to apm");
        ExportMetricsServiceRequest request =
                ExportMetricsServiceRequest
                        .newBuilder()
                        .addAllResourceMetrics(metricsData.getResourceMetricsList())
                        .build();
        ExportMetricsServiceResponse response = metricsServiceBlockingStub.export(request);
        if (response.hasPartialSuccess()) {
            log.info("Send metrics data to apm complete");
        } else {
            log.error("Failed to send metrics data to apm, response:{}", response);
        }
    }

    private void handleTrace(String inputJson) throws IOException {
        TracesData.Builder builder = TracesData.newBuilder();
        ProtoBufJsonUtils.fromJSON(inputJson, builder);
        TracesData tracesData = builder.build();
        log.info("Sending trace data to apm");
        ExportTraceServiceRequest request =
                ExportTraceServiceRequest
                        .newBuilder()
                        .addAllResourceSpans(tracesData.getResourceSpansList())
                        .build();
        ExportTraceServiceResponse response = traceServiceBlockingStub.export(request);
        if (response.hasPartialSuccess()) {
            log.info("Send trace data to apm complete");
        } else {
            log.error("Failed to send trace data to apm, response:{}", response);
        }
    }

    public static class Factory implements EmitterFactory {

        @Override
        public AbstractEmitter createEmitter(AbstractCache inputCache) {
            return new GrpcEmitter(inputCache);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        channel.shutdown();
        keepAliveThreadPool.shutdown();
    }
}

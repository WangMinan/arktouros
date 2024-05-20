package edu.npu.arktouros.receiver.otel;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelLogServiceImpl;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelMetricsServiceImpl;
import edu.npu.arktouros.receiver.otel.serviceImpl.OtelTraceServiceImpl;
import edu.npu.arktouros.service.otel.queue.LogQueueService;
import edu.npu.arktouros.service.otel.queue.MetricsQueueService;
import edu.npu.arktouros.service.otel.queue.TraceQueueService;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : 通过GRPC协议接收数据
 */
@Slf4j
public class OtelGrpcReceiver extends DataReceiver {

    private final int logAnalyzerNumber;
    private final int traceAnalyzerNumber;
    private final int metricsAnalyzerNumber;
    private final SinkService sinkService;
    private final int port;
    private final Server server;
    private ExecutorService logAnalyzerThreadPool;
    private ExecutorService traceAnalyzerThreadPool;
    private ExecutorService metricsAnalyzerThreadPool;

    public OtelGrpcReceiver(int logAnalyzerNumber,
                            int traceAnalyzerNumber,
                            int metricsAnalyzerNumber,
                            LogQueueService logQueueService,
                            TraceQueueService traceQueueService,
                            MetricsQueueService metricsQueueService,
                            SinkService sinkService, int grpcPort) {
        this.logAnalyzerNumber = logAnalyzerNumber;
        this.traceAnalyzerNumber = traceAnalyzerNumber;
        this.metricsAnalyzerNumber = metricsAnalyzerNumber;
        this.sinkService = sinkService;
        this.port = grpcPort;
        OtelMetricsAnalyzer.setQueueService(metricsQueueService);
        OtelLogAnalyzer.setQueueService(logQueueService);
        OtelTraceAnalyzer.setQueueService(traceQueueService);
        initAndStartAnalyzers();
        server = ServerBuilder.forPort(grpcPort)
                .addService(new OtelMetricsServiceImpl())
                .addService(new OtelLogServiceImpl())
                .addService(new OtelTraceServiceImpl())
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            log.info("OtelGrpcReceiver start to receive data, listening on port:{}", port);
        } catch (IOException e) {
            log.error("Grpc receiver start error", e);
            throw new ArktourosException(e, "Grpc receiver start error");
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                log.info("Grpc server is shutting down.");
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                logAnalyzerThreadPool.shutdown();
                traceAnalyzerThreadPool.shutdown();
                metricsAnalyzerThreadPool.shutdown();
                log.info("Grpc receiver stopped");
            } catch (InterruptedException e) {
                log.error("Grpc receiver failed to shutdown.");
                Thread.currentThread().interrupt();
                throw new ArktourosException(e, "Grpc receiver failed to shutdown.");
            }
        }
    }

    private void initAndStartAnalyzers() {
        ThreadFactory logAnalyzerThreadFactory = new BasicThreadFactory.Builder()
                .namingPattern("log-analyzer-%d").build();
        ThreadFactory traceAnalyzerThreadFactory = new BasicThreadFactory.Builder()
                .namingPattern("trace-analyzer-%d").build();
        ThreadFactory metricsAnalyzerThreadFactory = new BasicThreadFactory.Builder()
                .namingPattern("metrics-analyzer-%d").build();
        logAnalyzerThreadPool = new ThreadPoolExecutor(logAnalyzerNumber, logAnalyzerNumber,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(logAnalyzerNumber),
                logAnalyzerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        traceAnalyzerThreadPool = new ThreadPoolExecutor(traceAnalyzerNumber, traceAnalyzerNumber,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(traceAnalyzerNumber),
                traceAnalyzerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        metricsAnalyzerThreadPool = new ThreadPoolExecutor(metricsAnalyzerNumber, metricsAnalyzerNumber,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(metricsAnalyzerNumber),
                metricsAnalyzerThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        for (int i = 0; i < logAnalyzerNumber; i++) {
            OtelLogAnalyzer logAnalyzer = new OtelLogAnalyzer(sinkService);
            logAnalyzer.setName("OtelLogAnalyzer-" + i);
            logAnalyzer.init();
            logAnalyzerThreadPool.submit(logAnalyzer);
        }
        for (int i = 0; i < traceAnalyzerNumber; i++) {
            OtelTraceAnalyzer traceAnalyzer = new OtelTraceAnalyzer(sinkService);
            traceAnalyzer.setName("OtelTraceAnalyzer-" + i);
            traceAnalyzer.init();
            traceAnalyzerThreadPool.submit(traceAnalyzer);
        }
        for (int i = 0; i < metricsAnalyzerNumber; i++) {
            OtelMetricsAnalyzer metricsAnalyzer = new OtelMetricsAnalyzer(sinkService);
            metricsAnalyzer.setName("OtelMetricsAnalyzer-" + i);
            metricsAnalyzer.init();
            metricsAnalyzerThreadPool.submit(metricsAnalyzer);
        }
    }
}

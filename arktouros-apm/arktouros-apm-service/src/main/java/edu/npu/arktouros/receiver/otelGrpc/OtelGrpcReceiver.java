package edu.npu.arktouros.receiver.otelGrpc;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelLogServiceImpl;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelMetricsServiceImpl;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelTraceServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : 通过GRPC协议接收数据
 */
@Slf4j
public class OtelGrpcReceiver extends DataReceiver {

    private int port;

    private final Server server;

    public OtelGrpcReceiver(int grpcPort) {
        this.port = grpcPort;
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
            log.info("OtelGrpcReceiver start to start data, listening on port:{}", port);
        } catch (IOException e) {
            log.error("Grpc receiver start error", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                OtelLogAnalyzer.getInstance().interrupt();
                OtelMetricsAnalyzer.getInstance().interrupt();
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                log.info("Grpc receiver stopped");
            } catch (InterruptedException e) {
                log.error("Grpc receiver failed to shutdown.");
                throw new RuntimeException(e);
            }
        }
    }
}

package edu.npu.arktouros.receiver.grpc.arktouros;

import edu.npu.arktouros.model.exception.ArktourosException;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.grpc.arktouros.serviceImpl.ArktourosLogServiceImpl;
import edu.npu.arktouros.receiver.grpc.arktouros.serviceImpl.ArktourosMetricServiceImpl;
import edu.npu.arktouros.receiver.grpc.arktouros.serviceImpl.ArktourosSpanServiceImpl;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : Arktouros私有格式的otel数据接收器
 */
@Slf4j
public class ArktourosGrpcReceiver extends DataReceiver {

    private final int port;
    private final Server server;

    public ArktourosGrpcReceiver(SinkService sinkService, int port) {
        this.port = port;
        this.server = ServerBuilder
                .forPort(port)
                .addService(new ArktourosSpanServiceImpl(sinkService))
                .addService(new ArktourosMetricServiceImpl(sinkService))
                .addService(new ArktourosLogServiceImpl(sinkService))
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            log.info("ArktourosGrpcReceiver start to receive data, listening on port:{}", port);
        } catch (IOException e) {
            log.error("Grpc receiver start error", e);
            throw new ArktourosException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                log.info("Grpc server is shutting down.");
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                log.info("Grpc receiver stopped");
            } catch (InterruptedException e) {
                log.error("Grpc receiver failed to shutdown.");
                // 遇到InterruptedException异常，重新设置中断标志位
                Thread.currentThread().interrupt();
                throw new ArktourosException(e, "Grpc receiver failed to shutdown.");
            }
        }
    }
}

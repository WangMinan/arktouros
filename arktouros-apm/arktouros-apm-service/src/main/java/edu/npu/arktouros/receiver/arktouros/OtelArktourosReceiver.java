package edu.npu.arktouros.receiver.arktouros;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.arktouros.serviceImpl.OtelMetricServiceImpl;
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
public class OtelArktourosReceiver extends DataReceiver {

    private final int port;
    private final Server server;

    public OtelArktourosReceiver(SinkService sinkService, int port) {
        this.port = port;
        this.server = ServerBuilder
                .forPort(port)
                .addService(new OtelMetricServiceImpl(sinkService))
                .addService(new OtelMetricServiceImpl(sinkService))
                .addService(new OtelMetricServiceImpl(sinkService))
                .build();
    }

    @Override
    public void start() {
        try {
            server.start();
            log.info("OtelArktourosReceiver start to receive data, listening on port:{}", port);
        } catch (IOException e) {
            log.error("Grpc receiver start error", e);
            throw new RuntimeException(e);
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
                throw new RuntimeException(e);
            }
        }
    }
}

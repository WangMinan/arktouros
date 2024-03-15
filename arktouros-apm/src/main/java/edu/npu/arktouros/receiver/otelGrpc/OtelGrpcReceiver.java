package edu.npu.arktouros.receiver.otelGrpc;

import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelLogHandler;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelMetricsHandler;
import edu.npu.arktouros.receiver.otelGrpc.service.OtelTraceHandler;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author : [wangminan]
 * @description : 通过GRPC协议接收数据
 */
@Slf4j
public class OtelGrpcReceiver extends DataReceiver {
    @Value("${receiver.grpc.port}")
    private int PORT;

    private Server server;

    public OtelGrpcReceiver() {
        server = ServerBuilder.forPort(PORT)
                .addService(new OtelMetricsHandler())
                .addService(new OtelLogHandler())
                .addService(new OtelTraceHandler())
                .build();
    }

    @Override
    public void receive() {
        log.info("OtelGrpcReceiver start to receive data");
    }
}

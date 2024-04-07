package edu.npu.arktouros.receiver;

import edu.npu.arktouros.analyzer.otel.OtelLogAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelMetricsAnalyzer;
import edu.npu.arktouros.analyzer.otel.OtelTraceAnalyzer;
import edu.npu.arktouros.receiver.otel.OtelGrpcReceiver;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * @author : [wangminan]
 * @description : 数据接收器工厂
 */
@Component
@DependsOn("dataSource")
public class DataReceiverFactoryBean implements FactoryBean<DataReceiver> {

    @Value("${instance.active.dataReceiver}")
    private String activeDataReceiver;

    @Value("${receiver.grpc.port}")
    private int grpcPort;

    @Resource
    private OtelLogAnalyzer logAnalyzer;

    @Resource
    private OtelMetricsAnalyzer metricsAnalyzer;

    @Resource
    private OtelTraceAnalyzer traceAnalyzer;

    @Override
    public DataReceiver getObject() {
        if (activeDataReceiver.equals("otelGrpc")) {
            return new OtelGrpcReceiver(grpcPort, logAnalyzer, metricsAnalyzer, traceAnalyzer);
        } else {
            throw new IllegalArgumentException("can not find data receiver type from profile");
        }
    }

    @Override
    public Class<?> getObjectType() {
        return DataReceiver.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}

package edu.npu.arktouros.receiver;

import edu.npu.arktouros.receiver.otelGrpc.OtelGrpcReceiver;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author : [wangminan]
 * @description : 数据接收器工厂
 */
@Component
public class DataReceiverFactoryBean implements FactoryBean<DataReceiver> {

    @Value("${instance.active.dataReceiver}")
    private String activeDataReceiver;

    @Value("${receiver.grpc.port}")
    private int grpcPort;

    @Override
    public DataReceiver getObject() throws Exception {
        if (activeDataReceiver.equals("otelGrpc")) {
            return new OtelGrpcReceiver(grpcPort);
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

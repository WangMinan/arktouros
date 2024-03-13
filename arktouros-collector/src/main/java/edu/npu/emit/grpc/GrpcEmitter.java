package edu.npu.emit.grpc;

import edu.npu.cache.AbstractCache;
import edu.npu.emit.AbstractEmitter;
import edu.npu.properties.PropertiesProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author : [wangminan]
 * @description : [grpc发射器]
 */
public class GrpcEmitter extends AbstractEmitter {

    private final String HOST;
    private final int PORT;

    public GrpcEmitter(AbstractCache inputCache) {
        super(inputCache);
        HOST = PropertiesProvider.getProperty("emitter.grpc.host");
        PORT = Integer.parseInt(PropertiesProvider.getProperty("emitter.grpc.port"));
    }
}

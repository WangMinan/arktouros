package edu.npu.arktouros.config;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.cache.CacheFactory;
import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.emitter.AbstractEmitter;
import edu.npu.arktouros.emitter.EmitterFactory;
import edu.npu.arktouros.emitter.grpc.arktouros.ArktourosGrpcEmitter;
import edu.npu.arktouros.emitter.grpc.otel.OtelGrpcEmitter;
import edu.npu.arktouros.preHandler.AbstractPreHandler;
import edu.npu.arktouros.preHandler.JsonLogPreHandler;
import edu.npu.arktouros.preHandler.PreHandlerFactory;
import edu.npu.arktouros.receiver.AbstractReceiver;
import edu.npu.arktouros.receiver.JsonLogFileReceiver;
import edu.npu.arktouros.receiver.ReceiverFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * @author : [wangminan]
 * @description : 实例选择器
 */
public class InstanceProvider {

    protected static String cacheClassName;
    protected static String receiverClassName;
    protected static String preHandlerClassName;
    protected static String emitterClassName;
    protected static AbstractCache receiverOutputCache; // 也是 preHandler的inputCache
    protected static AbstractCache preHandlerOutputCache; // 也是 emitter的inputCache

    private InstanceProvider() {
        throw new UnsupportedOperationException("InstanceProvider is a utility class and should not be instantiated");
    }

    public static void init() {
        cacheClassName =
                PropertiesProvider.getProperty("instance.active.cache");
        receiverClassName =
                PropertiesProvider.getProperty("instance.active.receiver");
        preHandlerClassName =
                PropertiesProvider.getProperty("instance.active.preHandler");
        emitterClassName =
                PropertiesProvider.getProperty("instance.active.emitter");
        receiverOutputCache = getNewCache();
        preHandlerOutputCache = getNewCache();
    }


    public static AbstractReceiver getReceiver() {

        ReceiverFactory factory;
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(receiverClassName) &&
                receiverClassName.equals("JsonLogFileReceiver")) {
            factory = new JsonLogFileReceiver.Factory();
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
        return factory.createReceiver(receiverOutputCache);
    }

    public static AbstractPreHandler getPreHandler() {
        PreHandlerFactory factory;
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(preHandlerClassName) &&
                preHandlerClassName.equals("JsonLogPreHandler")) {
            factory = new JsonLogPreHandler.Factory();
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
        return factory.createPreHandler(receiverOutputCache, preHandlerOutputCache);
    }

    public static AbstractEmitter getEmitter() {
        EmitterFactory factory;
        if (StringUtils.isNotEmpty(emitterClassName) &&
                emitterClassName.equals("OtelGrpcEmitter")) {
            factory = new OtelGrpcEmitter.Factory();
        } else if (StringUtils.isNotEmpty(emitterClassName) &&
                emitterClassName.equals("ArktourosGrpcEmitter")) {
            factory = new ArktourosGrpcEmitter.Factory();
        } else {
            throw new IllegalArgumentException("Unknown emitter class: " + emitterClassName);
        }
        return factory.createEmitter(preHandlerOutputCache);
    }

    private static AbstractCache getNewCache() {
        CacheFactory factory;
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(cacheClassName) &&
                cacheClassName.equals("LogQueueCache")) {
            factory = new LogQueueCache.Factory();
        } else {
            throw new IllegalArgumentException("Unknown cache class: " + cacheClassName);
        }
        return factory.createCache();
    }
}

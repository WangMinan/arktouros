package edu.npu.arktouros.config;

import edu.npu.arktouros.cache.AbstractCache;
import edu.npu.arktouros.cache.CacheFactory;
import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.emitter.AbstractEmitter;
import edu.npu.arktouros.emitter.EmitterFactory;
import edu.npu.arktouros.emitter.grpc.GrpcEmitter;
import edu.npu.arktouros.preHandler.AbstractPreHandler;
import edu.npu.arktouros.preHandler.OtlpLogPreHandler;
import edu.npu.arktouros.preHandler.PreHandlerFactory;
import edu.npu.arktouros.receiver.AbstractReceiver;
import edu.npu.arktouros.receiver.OtlpLogFileReceiver;
import edu.npu.arktouros.receiver.ReceiverFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * @author : [wangminan]
 * @description : 实例选择器
 */
public class InstanceProvider {

    private static String cacheClassName;
    private static String receiverClassName;
    private static String preHandlerClassName;
    private static String emitterClassName;
    private static AbstractCache receiverOutputCache; // 也是 preHandler的inputCache
    private static AbstractCache preHandlerOutputCache; // 也是 emitter的inputCache

    public static void init() {
        cacheClassName =
                PropertiesProvider.getProperty("instance.cache");
        receiverClassName =
                PropertiesProvider.getProperty("instance.receiver");
        preHandlerClassName =
                PropertiesProvider.getProperty("instance.preHandler");
        emitterClassName =
                PropertiesProvider.getProperty("instance.emitter");
        receiverOutputCache = getNewCache();
        preHandlerOutputCache = getNewCache();
    }


    public static AbstractReceiver getReceiver() {

        ReceiverFactory factory;
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(receiverClassName) &&
                receiverClassName.equals("OtlpLogFileReceiver")) {
            factory = new OtlpLogFileReceiver.Factory();
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
        return factory.createReceiver(receiverOutputCache);
    }

    public static AbstractPreHandler getPreHandler() {
        PreHandlerFactory factory;
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(preHandlerClassName) &&
                preHandlerClassName.equals("OtlpLogPreHandler")) {
            factory = new OtlpLogPreHandler.Factory();
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
        return factory.createPreHandler(receiverOutputCache, preHandlerOutputCache);
    }

    public static AbstractEmitter getEmitter() {
        EmitterFactory factory;
        if (StringUtils.isNotEmpty(emitterClassName) &&
                emitterClassName.equals("GrpcEmitter")) {
            factory = new GrpcEmitter.Factory();
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

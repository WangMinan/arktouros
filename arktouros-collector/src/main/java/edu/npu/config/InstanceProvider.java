package edu.npu.config;

import edu.npu.cache.AbstractCache;
import edu.npu.cache.CacheFactory;
import edu.npu.cache.LogQueueCache;
import edu.npu.emit.AbstractEmitter;
import edu.npu.emit.EmitterFactory;
import edu.npu.emit.grpc.GrpcEmitter;
import edu.npu.preHandler.AbstractPreHandler;
import edu.npu.preHandler.OtlpLogPreHandler;
import edu.npu.preHandler.PreHandlerFactory;
import edu.npu.properties.PropertiesProvider;
import edu.npu.receiver.AbstractReceiver;
import edu.npu.receiver.OtlpLogFileReceiver;
import edu.npu.receiver.ReceiverFactory;
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
    }


    public static AbstractReceiver getReceiver() {
        receiverOutputCache = getNewCache();
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
        preHandlerOutputCache = getNewCache();
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

package edu.npu.config;

import edu.npu.cache.AbstractCache;
import edu.npu.cache.LogQueueCache;
import edu.npu.emit.AbstractEmitter;
import edu.npu.emit.grpc.GrpcEmitter;
import edu.npu.preHandler.AbstractPreHandler;
import edu.npu.preHandler.OtlpLogPreHandler;
import edu.npu.properties.PropertiesProvider;
import edu.npu.receiver.AbstractReceiver;
import edu.npu.receiver.OtlpLogFileReceiver;
import org.apache.commons.lang3.StringUtils;

/**
 * @author : [wangminan]
 * @description : 实例选择器
 */
public class InstanceProvider {

    private static String cacheClassName;

    private static String receiverClassName;

    private static String emitterClassName;

    private static AbstractCache receiverOutputCache; // 也是 preHandler的inputCache

    private static AbstractCache preHandlerOutputCache; // 也是 emitter的inputCache

    public static void init() {
        cacheClassName =
                PropertiesProvider.getProperty("instance.cache");
        receiverClassName =
                PropertiesProvider.getProperty("instance.receiver");
        emitterClassName =
                PropertiesProvider.getProperty("instance.emitter");
    }


    public static AbstractReceiver getReceiver() {
        receiverOutputCache = getNewCache();
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(receiverClassName) &&
                receiverClassName.equals("OtlpLogFileReceiver")) {
            return new OtlpLogFileReceiver(receiverOutputCache);
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
    }

    public static AbstractPreHandler getPreHandler() {
        preHandlerOutputCache = getNewCache();
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(receiverClassName) &&
                receiverClassName.equals("OtlpLogPreHandler")) {
            return new OtlpLogPreHandler(receiverOutputCache, preHandlerOutputCache);
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
    }

    public static AbstractEmitter getEmitter() {
        if (StringUtils.isNotEmpty(emitterClassName) &&
                emitterClassName.equals("GrpcEmitter")) {
            return new GrpcEmitter(preHandlerOutputCache);
        } else {
            throw new IllegalArgumentException("Unknown emitter class: " + emitterClassName);
        }
    }

    private static <T> AbstractCache getNewCache() {
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(cacheClassName) &&
                cacheClassName.equals("LogQueueCache")) {
            return new LogQueueCache();
        } else {
            throw new IllegalArgumentException("Unknown cache class: " + cacheClassName);
        }
    }
}

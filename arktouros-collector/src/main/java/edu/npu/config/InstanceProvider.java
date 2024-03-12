package edu.npu.config;

import edu.npu.cache.AbstractCache;
import edu.npu.cache.LogQueueCache;
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

    public static void init() {
        cacheClassName =
                PropertiesProvider.getProperty("instance.cache");
        receiverClassName =
                PropertiesProvider.getProperty("instance.receiver");
    }


    public static AbstractReceiver getReceiver() {
        AbstractCache cache = getNewCache();
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(receiverClassName) &&
                receiverClassName.equals("OtlpLogFileReceiver")) {
            return new OtlpLogFileReceiver(cache);
        } else {
            throw new IllegalArgumentException("Unknown receiver class: " + receiverClassName);
        }
    }

    public static AbstractCache getNewCache() {
        // 避免反射 手动加载
        if (StringUtils.isNotEmpty(cacheClassName) &&
                cacheClassName.equals("LogQueueCache")) {
            return new LogQueueCache();
        } else {
            throw new IllegalArgumentException("Unknown cache class: " + cacheClassName);
        }
    }
}

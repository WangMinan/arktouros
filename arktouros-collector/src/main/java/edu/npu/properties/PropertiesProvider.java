package edu.npu.properties;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author : [wangminan]
 * @description : 从配置文件中读取配置
 */
@Slf4j
public class PropertiesProvider {

    private static Properties properties;

    public static void init() {
        // 从resources目录下读取配置文件 application.yaml
        try (InputStream propertiesFileInputStream =
                     PropertiesProvider.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(propertiesFileInputStream);
        } catch (IOException e) {
            log.error("Failed to load properties file", e);
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}

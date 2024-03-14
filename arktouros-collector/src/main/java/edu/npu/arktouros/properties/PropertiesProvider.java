package edu.npu.arktouros.properties;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 从配置文件中读取配置
 */
@Slf4j
public class PropertiesProvider {

private static Map<String, Object> map;

    public static void init() {
        Yaml yaml = new Yaml();
        // 从resources目录下读取配置文件 application.yaml
        try (InputStream propertiesFileInputStream =
                     PropertiesProvider.class.getResourceAsStream("/application.yaml")) {
            map = yaml.load(propertiesFileInputStream);
        } catch (IOException e) {
            log.error("Failed to load properties file", e);
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String propertyPath) {
        String[] keys = propertyPath.split("\\.");
        Map<String, Object> value = map;

        for (String key : keys) {
            // Check if the key exists and is not the last key
            if (value.containsKey(key) && value.get(key) instanceof Map) {
                value = (Map<String, Object>) value.get(key);
            } else {
                return String.valueOf(value.get(key));
            }
        }
        return null; // or throw an exception if the property does not exist
    }
}

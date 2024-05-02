package edu.npu.arktouros.config;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author : [wangminan]
 * @description : 从配置文件中读取配置
 */
@Slf4j
public class PropertiesProvider {

    private static Map<String, Object> map;

    private PropertiesProvider() {
    }

    public static void init() {
        Yaml yaml = new Yaml();
        // 读取配置文件 application.yaml
        // 优先级  {user.dir}/config/application.yaml > resource/application.yaml
        try (InputStream propertiesFileInputStream =
                     Files.newInputStream(
                             Paths.get(System.getProperty("user.dir"),
                                     "config", "application.yaml"))) {
            map = yaml.load(propertiesFileInputStream);
        } catch (IOException e) {
            log.warn("Failed to load properties file from config/application.yaml. Trying to find it from resource dir");
            try (InputStream propertiesFileInputStream =
                         PropertiesProvider.class.getResourceAsStream("/application.yaml")) {
                map = yaml.load(propertiesFileInputStream);
            } catch (IOException ex) {
                log.error("""
                        Failed to load properties file from resource, please check the location of your config file.
                        """, ex);
                throw new RuntimeException(ex);
            }
        }
    }

    public static String getProperty(String propertyPath) {
        return getProperty(propertyPath, null);
    }

    public static String getProperty(String propertyPath, String defaultValue) {
        Map<String, Object> value = map;
        for (String key : propertyPath.split("\\.")) {
            if (!value.containsKey(key)) return defaultValue;
            Object obj = value.get(key);
            if (obj instanceof Map) {
                value = (Map<String, Object>) obj;
            } else {
                String result = String.valueOf(obj);
                if (result.isEmpty()) {
                    return defaultValue;
                } else {
                    return result;
                }
            }
        }
        return defaultValue;
    }
}

package edu.npu.properties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * @author : [wangminan]
 * @description : 从配置文件中读取配置
 */
@Slf4j
public class PropertiesProvider {

    private static LinkedHashMap configMap;

    public static void init() {
        // 从resources目录下读取配置文件 application.yaml
        try (InputStream propertiesFileInputStream =
                     PropertiesProvider.class.getResourceAsStream("/application.yaml")) {
            Yaml yaml = new Yaml();
            configMap =
                    yaml.load(propertiesFileInputStream);
        } catch (IOException e) {
            log.error("Failed to load properties file", e);
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String key) {
        String[] split = key.split("\\.");
        LinkedHashMap tempMap = configMap;
        for (int i = 0; i < split.length - 1; i++) {
            tempMap = (LinkedHashMap) tempMap.get(split[i]);
        }
        return (String) tempMap.get(split[split.length - 1]);
    }
}

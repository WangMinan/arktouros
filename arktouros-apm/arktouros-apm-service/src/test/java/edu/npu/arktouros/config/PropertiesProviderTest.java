package edu.npu.arktouros.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author : [wangminan]
 * @description : {@link PropertiesProvider}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PropertiesProviderTest {

    @Test
    @Timeout(10)
    void testInit() throws IOException {
        // 创建临时文件 System.getProperty("user.dir"),
        //                                     "config", "application.yaml")
        Files.createDirectory(Paths.get(System.getProperty("user.dir"),
                "config"));
        Files.createFile(Paths.get(System.getProperty("user.dir"),
                "config", "application.yaml"));
        PropertiesProvider.init();
        // 清除
        Files.deleteIfExists(Paths.get(System.getProperty("user.dir"),
                "config", "application.yaml"));
        // 清除config
        Files.deleteIfExists(Paths.get(System.getProperty("user.dir"),
                "config"));
    }

    @Test
    @Timeout(10)
    void testGetProperty() {
        PropertiesProvider.init();
        Assertions.assertEquals(
                PropertiesProvider.getProperty("instance.active.sinker"), "elasticsearch");
        Assertions.assertEquals(
                PropertiesProvider.getProperty("server.host", "host"),
                "host");
        Assertions.assertNotNull(
                PropertiesProvider.getProperty("testAttr", "test"),
                "test");
    }
}

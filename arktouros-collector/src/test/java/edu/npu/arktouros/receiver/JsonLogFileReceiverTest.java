package edu.npu.arktouros.receiver;

import edu.npu.arktouros.cache.LogQueueCache;
import edu.npu.arktouros.config.PropertiesProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * @author : [wangminan]
 * @description : {@link JsonLogFileReceiver}
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsonLogFileReceiverTest {

    private static JsonLogFileReceiver receiver;
    private static LogQueueCache outputCache;
    private static MockedStatic<PropertiesProvider> providerMockedStatic;
    private final Path otelLogsTest = Path.of("otel_logs");
    private final String content = "2024-05-15 00:00:00.000 [main] INFO  edu.npu.arktouros.config.PropertiesProvider - test";

    @BeforeAll
    static void beforeAll() {
        providerMockedStatic = Mockito.mockStatic(PropertiesProvider.class);
        outputCache = new LogQueueCache();
    }

    @BeforeEach
    void beforeEach() throws IOException {
        receiver = new JsonLogFileReceiver(outputCache);
        FileUtils.deleteDirectory(new File("otel_logs"));
    }

    @AfterAll
    static void afterAll() {
        providerMockedStatic.close();
    }

    @Test
    void testFactory() {
        ReceiverFactory factory = new JsonLogFileReceiver.Factory();
        Assertions.assertNotNull(factory.createReceiver(outputCache));
    }

    @Test
    void testRun() throws IOException, InterruptedException {
        log.info(PropertiesProvider.getProperty("test"));
        Files.createDirectory(otelLogsTest);
        // 在otelLogsTest下新建log1.txt
        File log1 = new File(otelLogsTest.toFile(), "log1.txt");
        File log2 = new File(otelLogsTest.toFile(), "log2.txt");
        Files.write(log1.toPath(), content.getBytes());
        Files.write(log2.toPath(), content.getBytes());
        receiver.start();
        CountDownLatch latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);
        receiver.interrupt();
        // 重置
        receiver = new JsonLogFileReceiver(outputCache);
        // 写入新文件
        File log3 = new File(otelLogsTest.toFile(), "log3.txt");
        Files.write(log3.toPath(), content.getBytes());
        receiver.start();
        latch = new CountDownLatch(1);
        latch.await(5, TimeUnit.SECONDS);
        receiver.interrupt();
    }

    @Test
    void testRunError1() throws InterruptedException {
        try {
            Assertions.assertDoesNotThrow(() -> receiver.start());
        } catch (Exception e) {
            log.info("Normal exception: {}", e.getMessage());
        }
        Thread.sleep(2000);
    }

    @Test
    void testRunError2() throws InterruptedException {
        providerMockedStatic.when(() -> PropertiesProvider.getProperty(
                        ArgumentMatchers.eq("receiver.otlpLogFile.logDir")))
                .thenReturn("");
        try {
            Assertions.assertDoesNotThrow(() -> receiver.start());
        } catch (Exception e) {
            log.info("Ignore this exception: {}", e.getMessage());
        }
        Thread.sleep(2000);
    }

    @Test
    void testPrepareIf() {
        providerMockedStatic.when(() -> PropertiesProvider.getProperty(
                        ArgumentMatchers.eq("receiver.otlpLogFile.logDir")))
                .thenReturn("");
        Assertions.assertThrows(Exception.class, () -> receiver.prepare());
        providerMockedStatic.when(() -> PropertiesProvider.getProperty(
                        ArgumentMatchers.eq("receiver.otlpLogFile.logDir")))
                .thenReturn(null);
        Assertions.assertThrows(Exception.class, () -> receiver.prepare());
    }

    @Test
    void testPrepareError() throws IOException {
        providerMockedStatic.when(() -> PropertiesProvider.getProperty(
                        ArgumentMatchers.eq("receiver.otlpLogFile.logDir")))
                .thenReturn("otel_logs");
        Files.createDirectory(otelLogsTest);
        Assertions.assertThrows(RuntimeException.class, () -> receiver.prepare());
    }
}

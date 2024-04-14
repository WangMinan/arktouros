package edu.npu.arktouros;

import edu.npu.arktouros.config.PropertiesProvider;
import edu.npu.arktouros.receiver.DataReceiver;
import edu.npu.arktouros.service.otel.sinker.SinkService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author : [wangminan]
 * @description : APM启动类
 */
@SpringBootApplication
@Slf4j
@EnableRetry
public class ApmMain implements CommandLineRunner {

    @Resource
    private DataReceiver dataReceiver;

    @Resource
    private SinkService sinkService;

    @Override
    public void run(String... args) {
        log.info("APM starting, adding shutdown hook.");
        PropertiesProvider.init();
        sinkService.init();
        if (!sinkService.isReady()) {
            log.error("APM sink service is not ready, shutting down.");
            return;
        }
        // 拉起数据接收器 接收器会自动调用analyzer analyzer会自动调用sinker
        dataReceiver.start();
        Thread thread = new Thread(
                () -> {
                    // 停止数据接收器
                    dataReceiver.stop();
                    log.info("APM shutting down");
                }
        );
        thread.setName("APM-shutdown-thread");
        Runtime.getRuntime().addShutdownHook(thread);
    }

    public static void main(String[] args) {
        SpringApplication.run(ApmMain.class, args);
    }
}

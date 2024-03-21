package edu.npu.arktouros;

import edu.npu.arktouros.receiver.DataReceiver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author : [wangminan]
 * @description : APM启动类
 */
@SpringBootApplication
@Slf4j
public class ApmMain implements CommandLineRunner {

    @Resource
    private DataReceiver dataReceiver;

    @Override
    public void run(String... args) {
        log.info("APM starting");
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

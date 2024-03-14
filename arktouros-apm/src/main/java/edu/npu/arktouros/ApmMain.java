package edu.npu.arktouros;

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

    @Override
    public void run(String... args) throws Exception {
        log.info("APM starting");
    }

    public static void main(String[] args) {
        SpringApplication.run(ApmMain.class, args);
    }
}

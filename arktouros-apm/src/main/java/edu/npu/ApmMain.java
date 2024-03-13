package edu.npu;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author : [wangminan]
 * @description : APM启动类
 */
@SpringBootApplication
public class ApmMain implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // do nothing
    }

    public static void main(String[] args) {
        SpringApplication.run(ApmMain.class, args);
    }
}

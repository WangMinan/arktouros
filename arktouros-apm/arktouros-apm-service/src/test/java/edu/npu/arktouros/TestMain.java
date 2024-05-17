package edu.npu.arktouros;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @author : [wangminan]
 * @description : 为测试类提供springboot环境
 */
@SpringBootApplication
@Slf4j
@EnableRetry
public class TestMain {
    public static void main(String[] args) {
        log.info("This is the main springboot class for test environment.");
    }
}

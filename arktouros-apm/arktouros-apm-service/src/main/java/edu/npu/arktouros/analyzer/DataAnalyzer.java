package edu.npu.arktouros.analyzer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : 数据处理器
 */
@Slf4j
public abstract class DataAnalyzer extends Thread {

    public abstract void init();

    @Override
    public void run() {
        log.info("DataAnalyzer start.");
    }

    @Override
    public void interrupt() {
        super.interrupt();
    }
}

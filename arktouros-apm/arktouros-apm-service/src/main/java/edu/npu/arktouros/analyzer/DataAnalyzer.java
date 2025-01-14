package edu.npu.arktouros.analyzer;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : [wangminan]
 * @description : 数据处理器
 */
@Slf4j
@Setter
public abstract class DataAnalyzer extends Thread {

    protected boolean needCleanWhileShutdown = false;

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

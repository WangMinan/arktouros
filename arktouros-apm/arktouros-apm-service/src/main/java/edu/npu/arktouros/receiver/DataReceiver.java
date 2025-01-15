package edu.npu.arktouros.receiver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DataReceiver {

    protected boolean isRunning;

    public void start() {
        isRunning = true;
    }

    // 先清理环境 然后启动 用于删除数据后启动
    public void flushAndStart() {
        start();
    }

    // 不清理analyzer中的数据
    public void stop() {
        isRunning = false;
    }

    // 主要作用是确保analyzer中的数据被清理
    public void stopAndClean() {
        log.info("Stop and clean data receiver");
        stop();
    }
}

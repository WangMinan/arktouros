package edu.npu.arktouros.receiver;

public abstract class DataReceiver {

    public abstract void start();

    // 先清理环境 然后启动 用于删除数据后启动
    public abstract void flushAndStart();

    public abstract void stop();

    public void stopGracefully() {
        stop();
    }
}

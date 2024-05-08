package edu.npu.arktouros.analyzer;

/**
 * @author : [wangminan]
 * @description : 数据处理器
 */
public abstract class DataAnalyzer extends Thread {

    public abstract void init();

    @Override
    public void run() {
        super.run();
    }
}

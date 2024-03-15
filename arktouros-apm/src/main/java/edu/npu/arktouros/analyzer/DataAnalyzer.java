package edu.npu.arktouros.analyzer;

import edu.npu.arktouros.sink.DataOperation;
import jakarta.annotation.Resource;

/**
 * @author : [wangminan]
 * @description : 数据处理器
 */
public abstract class DataAnalyzer {

    @Resource
    protected DataOperation dataOperation;

    public abstract void analyze();
}

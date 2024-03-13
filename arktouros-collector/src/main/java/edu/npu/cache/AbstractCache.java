package edu.npu.cache;

/**
 * @author : [wangminan]
 * @description : 存储的抽象类
 */
public abstract class AbstractCache {

    public abstract void put(String line);

    public abstract String get();
}

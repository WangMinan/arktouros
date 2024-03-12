package edu.npu.cache;

/**
 * @author : [wangminan]
 * @description : 存储的抽象类
 */
public abstract class AbstractCache {

    public abstract void put(Object object);

    public abstract Object get();
}

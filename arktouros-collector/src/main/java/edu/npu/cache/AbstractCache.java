package edu.npu.cache;

import java.util.Queue;

/**
 * @author : [wangminan]
 * @description : 存储的抽象类
 */
public abstract class AbstractCache<T> {

    public void put(T t) {}

    public T get() {
        return null;
    }
}

package edu.npu.arktouros.cache;

/**
 * @author : [wangminan]
 * @description : 存储的抽象类
 */
public interface AbstractCache {

    void put(String line);

    String get();
}

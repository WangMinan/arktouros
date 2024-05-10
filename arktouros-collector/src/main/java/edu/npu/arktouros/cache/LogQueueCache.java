package edu.npu.arktouros.cache;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author : [wangminan]
 * @description : 日志队列缓存
 */
public class LogQueueCache implements AbstractCache {

    private static final int DEFAULT_CAPACITY = 1000;

    // 阻塞队列 不需要考虑并发问题 用synchronize或者lock画蛇添足会导致线程阻塞
    protected static ArrayBlockingQueue<String> queue =
            new ArrayBlockingQueue<>(DEFAULT_CAPACITY);


    public void put(String object) {
        try {
            queue.put(object);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String get() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public static class Factory implements CacheFactory{
        @Override
        public AbstractCache createCache() {
            return new LogQueueCache();
        }
    }
}

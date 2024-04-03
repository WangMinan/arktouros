package edu.npu.arktouros.cache;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
public class LogQueueCache implements AbstractCache {

    private static final int DEFAULT_CAPACITY = 1000;

    private final ArrayBlockingQueue<String> queue =
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

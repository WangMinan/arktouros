package edu.npu.cache;

import lombok.SneakyThrows;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author : [wangminan]
 * @description : [一句话描述该类的功能]
 */
public class LogQueueCache extends AbstractCache {

    private static final int DEFAULT_CAPACITY = 1000;
    private static final int TAKE_TIMEOUT = 3;

    private final BlockingQueue<Object> queue =
            new ArrayBlockingQueue<>(DEFAULT_CAPACITY);

    @Override
    @SneakyThrows(InterruptedException.class)
    public void put(Object object) {
        if (object == null) {
            return;
        }
        queue.put(object);
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public Object get() {
        return queue.poll(TAKE_TIMEOUT, TimeUnit.SECONDS);
    }
}

package edu.npu.cache;

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

    private final BlockingQueue<String> queue =
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
            return queue.poll(TAKE_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

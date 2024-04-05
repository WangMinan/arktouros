package edu.npu.arktouros.service.otel.queue;

public interface QueueService <T> {

    void put(T t);

    T get();

    T get(boolean removeAtSameTime);

    boolean isEmpty();

    long size();
}

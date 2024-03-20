package edu.npu.arktouros.service.otel.queue;

public interface QueueService <T> {

    void put(T t);

    T get();

    boolean isEmpty();

    long size();
}

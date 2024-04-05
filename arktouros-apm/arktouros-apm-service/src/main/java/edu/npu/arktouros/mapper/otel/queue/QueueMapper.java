package edu.npu.arktouros.mapper.otel.queue;

public abstract class QueueMapper<T> {

    public abstract void add(T t);

    public abstract T getTop();

    public abstract boolean isEmpty();

    public abstract long getSize();

    public abstract void removeTop();
}

package edu.npu.arktouros.service.queue;

import lombok.Getter;

@Getter
public abstract class QueueService <T> {

    protected String name;

    abstract void put(T t);

    abstract T get();

    abstract T get(boolean removeAtSameTime);

    abstract boolean isEmpty();

    abstract long size();

    abstract void waitTableReady();

    abstract void clear();
}

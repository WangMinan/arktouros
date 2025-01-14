package edu.npu.arktouros.service.queue;

import lombok.Getter;

@Getter
public abstract class QueueService <T> {

    protected String name;

    public abstract void put(T t);

    public abstract T get();

    public abstract T get(boolean removeAtSameTime);

    public abstract boolean isEmpty();

    public abstract long size();

    public abstract void waitTableReady();

    public abstract void clear();
}

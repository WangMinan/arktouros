package edu.npu.arktouros.service.otel.sinker;

public abstract class SinkService<T> {
    public abstract void sink(T data);
}

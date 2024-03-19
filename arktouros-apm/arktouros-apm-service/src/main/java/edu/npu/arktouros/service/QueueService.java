package edu.npu.arktouros.service;

public interface QueueService <T> {

    void put(T t);

    void get(T t);

    boolean isEmpty();

    long size();
}

package com.commerce.platform.saga;

public interface SagaStep<T> {
    void process(T data);
    void rollback(T data);
}

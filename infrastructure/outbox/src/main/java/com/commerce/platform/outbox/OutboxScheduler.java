package com.commerce.platform.outbox;

public interface OutboxScheduler {
    void processOutboxMessage();
}

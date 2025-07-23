package com.commerce.platform.saga;

public enum SagaStatus {
    STARTED, PROCESSING, SUCCEEDED, COMPENSATING, COMPENSATED, FAILED
}

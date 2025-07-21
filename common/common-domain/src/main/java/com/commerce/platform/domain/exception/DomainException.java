package com.commerce.platform.domain.exception;

import com.commerce.platform.domain.event.DomainEvent;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.commerce.platform.product.service.domain.exception;

import com.commerce.platform.domain.exception.DomainException;

public class ProductApplicationServiceException extends DomainException {
    public ProductApplicationServiceException(String message) {
        super(message);
    }

    public ProductApplicationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

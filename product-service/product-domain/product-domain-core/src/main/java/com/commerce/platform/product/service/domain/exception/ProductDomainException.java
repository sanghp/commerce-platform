package com.commerce.platform.product.service.domain.exception;

import com.commerce.platform.domain.exception.DomainException;

public class ProductDomainException extends DomainException {

    public ProductDomainException(String message) {
        super(message);
    }

    public ProductDomainException(String message, Throwable cause) {
        super(message, cause);
    }
} 
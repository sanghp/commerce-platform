package com.commerce.platform.order.service.dataaccess.outbox.product.exception;

public class ProductReservationOutboxNotFoundException extends RuntimeException {

    public ProductReservationOutboxNotFoundException(String message) {
        super(message);
    }
}

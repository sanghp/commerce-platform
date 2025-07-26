package com.commerce.platform.product.service.domain.outbox;

public enum ProductOutboxEventType {
    
    PRODUCT_RESERVATION_RESPONSE("product-reservation-response");
    
    private final String value;
    
    ProductOutboxEventType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
} 
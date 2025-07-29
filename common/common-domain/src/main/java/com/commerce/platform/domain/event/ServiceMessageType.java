package com.commerce.platform.domain.event;

/**
 * 서비스 간 통신에 사용되는 메시지 타입을 정의
 * SAGA 패턴 구현을 위한 Command/Response 메시지 타입
 */
public enum ServiceMessageType {
    
    // Product Service Messages
    PRODUCT_RESERVATION_REQUEST("product-reservation-request"),
    PRODUCT_RESERVATION_RESPONSE("product-reservation-response");
    
    // Order Service Messages (추후 추가 예정)
    // ORDER_APPROVAL_REQUEST, ORDER_APPROVAL_RESPONSE
    
    // Payment Service Messages (추후 추가 예정)
    // PAYMENT_REQUEST, PAYMENT_RESPONSE
    
    // Customer Service Messages (추후 추가 예정)
    // CUSTOMER_VALIDATION_REQUEST, CUSTOMER_VALIDATION_RESPONSE
    
    private final String value;
    
    ServiceMessageType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
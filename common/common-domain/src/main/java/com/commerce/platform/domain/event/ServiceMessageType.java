package com.commerce.platform.domain.event;

/**
 * 서비스 간 통신에 사용되는 메시지 타입을 정의
 * SAGA 패턴 구현을 위한 Command/Response 메시지 타입
 */
public enum ServiceMessageType {
    
    // Product Service Messages
    PRODUCT_RESERVATION_REQUEST,
    PRODUCT_RESERVATION_RESPONSE,
    
    // Payment Service Messages
    PAYMENT_REQUEST,
    PAYMENT_RESPONSE;
    
    // Customer Service Messages (추후 추가 예정)
    // CUSTOMER_VALIDATION_REQUEST, CUSTOMER_VALIDATION_RESPONSE

}
package com.commerce.platform.payment.service.domain.ports.input.service;

import com.commerce.platform.payment.service.domain.dto.PaymentResponse;
import java.util.UUID;

public interface PaymentApplicationService {
    
    PaymentResponse getPaymentById(UUID paymentId);
    
    PaymentResponse getPaymentByOrderId(UUID orderId);
}
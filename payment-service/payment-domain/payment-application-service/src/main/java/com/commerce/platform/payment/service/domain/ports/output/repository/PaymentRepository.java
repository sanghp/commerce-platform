package com.commerce.platform.payment.service.domain.ports.output.repository;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.valueobject.PaymentId;

import java.util.Optional;

public interface PaymentRepository {
    
    Payment save(Payment payment);
    
    Optional<Payment> findById(PaymentId paymentId);
    
    Optional<Payment> findByOrderId(OrderId orderId);
}
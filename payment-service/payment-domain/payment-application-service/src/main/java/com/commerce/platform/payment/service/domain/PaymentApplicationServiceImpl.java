package com.commerce.platform.payment.service.domain;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.domain.dto.PaymentResponse;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import com.commerce.platform.payment.service.domain.ports.input.service.PaymentApplicationService;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentRepository;
import com.commerce.platform.payment.service.domain.valueobject.PaymentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
class PaymentApplicationServiceImpl implements PaymentApplicationService {
    
    private final PaymentRepository paymentRepository;
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Optional<Payment> paymentResponse = paymentRepository.findById(new PaymentId(paymentId));
        
        if (paymentResponse.isEmpty()) {
            log.error("Payment with id: {} not found", paymentId);
            throw new PaymentDomainException("Payment with id: " + paymentId + " not found");
        }
        
        Payment payment = paymentResponse.get();
        return buildPaymentResponse(payment);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Optional<Payment> paymentResponse = paymentRepository.findByOrderId(new OrderId(orderId));
        
        if (paymentResponse.isEmpty()) {
            log.error("Payment with order id: {} not found", orderId);
            throw new PaymentDomainException("Payment with order id: " + orderId + " not found");
        }
        
        Payment payment = paymentResponse.get();
        return buildPaymentResponse(payment);
    }
    
    private PaymentResponse buildPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId().getValue())
                .orderId(payment.getOrderId().getValue())
                .customerId(payment.getCustomerId().getValue())
                .price(payment.getPrice().getAmount())
                .createdAt(payment.getCreatedAt())
                .paymentStatus(payment.getPaymentStatus())
                .failureMessages(payment.getFailureMessages())
                .build();
    }
}
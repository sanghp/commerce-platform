package com.commerce.platform.payment.service.dataaccess.payment.mapper;

import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.dataaccess.payment.entity.PaymentEntity;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.valueobject.PaymentId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;

@Component
public class PaymentDataAccessMapper {
    
    public PaymentEntity paymentToPaymentEntity(Payment payment) {
        return PaymentEntity.builder()
                .id(payment.getId().getValue())
                .orderId(payment.getOrderId().getValue())
                .customerId(payment.getCustomerId().getValue())
                .price(payment.getPrice().getAmount())
                .paymentStatus(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .failureMessages(payment.getFailureMessages() != null
                        ? String.join(",", payment.getFailureMessages())
                        : null)
                .build();
    }
    
    public Payment paymentEntityToPayment(PaymentEntity paymentEntity) {
        return Payment.builder()
                .paymentId(new PaymentId(paymentEntity.getId()))
                .orderId(new OrderId(paymentEntity.getOrderId()))
                .customerId(new CustomerId(paymentEntity.getCustomerId()))
                .price(new Money(paymentEntity.getPrice()))
                .paymentStatus(paymentEntity.getPaymentStatus())
                .createdAt(paymentEntity.getCreatedAt())
                .failureMessages(paymentEntity.getFailureMessages() != null && !paymentEntity.getFailureMessages().isEmpty()
                        ? new ArrayList<>(Arrays.asList(paymentEntity.getFailureMessages().split(",")))
                        : new ArrayList<>())
                .build();
    }
}
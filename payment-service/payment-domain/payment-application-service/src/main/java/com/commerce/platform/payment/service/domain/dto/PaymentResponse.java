package com.commerce.platform.payment.service.domain.dto;

import com.commerce.platform.domain.valueobject.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private UUID paymentId;
    private UUID customerId;
    private BigDecimal price;
    private ZonedDateTime createdAt;
    private PaymentStatus paymentStatus;
    private List<String> failureMessages;
}
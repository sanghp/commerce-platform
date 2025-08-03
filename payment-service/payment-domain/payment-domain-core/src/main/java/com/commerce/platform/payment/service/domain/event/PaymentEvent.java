package com.commerce.platform.payment.service.domain.event;

import com.commerce.platform.domain.event.DomainEvent;
import com.commerce.platform.payment.service.domain.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public abstract class PaymentEvent implements DomainEvent<Payment> {
    private Payment payment;
    private ZonedDateTime createdAt;
    private List<String> failureMessages;
}

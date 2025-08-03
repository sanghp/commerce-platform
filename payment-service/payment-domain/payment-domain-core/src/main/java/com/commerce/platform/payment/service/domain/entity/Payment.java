package com.commerce.platform.payment.service.domain.entity;

import com.commerce.platform.domain.entity.AggregateRoot;
import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.PaymentStatus;
import com.commerce.platform.payment.service.domain.valueobject.PaymentId;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import com.commerce.platform.domain.util.UuidGenerator;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
public class Payment extends AggregateRoot<PaymentId> {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Money price;
    
    private PaymentStatus paymentStatus;
    private ZonedDateTime createdAt;
    private List<String> failureMessages;
    
    @Builder
    public Payment(PaymentId paymentId,
                   OrderId orderId,
                   CustomerId customerId,
                   Money price,
                   PaymentStatus paymentStatus,
                   ZonedDateTime createdAt,
                   List<String> failureMessages) {
        super.setId(paymentId);
        this.orderId = orderId;
        this.customerId = customerId;
        this.price = price;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.failureMessages = failureMessages;
    }
    
    public void initializePayment() {
        setId(new PaymentId(UuidGenerator.generate()));
        createdAt = ZonedDateTime.now(ZoneOffset.UTC);
        paymentStatus = PaymentStatus.PENDING;
    }
    
    public void validatePayment() {
        validateInitialPayment();
        validatePrice();
    }
    
    public void completePayment() {
        if (paymentStatus != PaymentStatus.PENDING) {
            throw new PaymentDomainException("Payment is not in correct state for complete operation!");
        }
        paymentStatus = PaymentStatus.COMPLETED;
    }
    
    public void cancelPayment(List<String> failureMessages) {
        if (paymentStatus != PaymentStatus.COMPLETED && paymentStatus != PaymentStatus.PENDING) {
            throw new PaymentDomainException("Payment is not in correct state for cancel operation!");
        }
        paymentStatus = PaymentStatus.CANCELLED;
        updateFailureMessages(failureMessages);
    }
    
    public void failPayment(List<String> failureMessages) {
        if (paymentStatus != PaymentStatus.PENDING) {
            throw new PaymentDomainException("Payment is not in correct state for fail operation!");
        }
        paymentStatus = PaymentStatus.FAILED;
        updateFailureMessages(failureMessages);
    }
    
    private void validateInitialPayment() {
        if (paymentStatus != null || getId() != null) {
            throw new PaymentDomainException("Payment is not in correct state for initialization!");
        }
    }
    
    private void validatePrice() {
        if (price == null || !price.isGreaterThanZero()) {
            throw new PaymentDomainException("Price must be greater than zero!");
        }
    }
    
    private void updateFailureMessages(List<String> failureMessages) {
        if (this.failureMessages != null && failureMessages != null) {
            this.failureMessages.addAll(failureMessages.stream().filter(message -> !message.isEmpty()).toList());
        }
        if (this.failureMessages == null) {
            this.failureMessages = failureMessages;
        }
    }
}

package com.commerce.platform.payment.service.domain;

import com.commerce.platform.payment.service.domain.entity.Credit;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.event.PaymentCancelledEvent;
import com.commerce.platform.payment.service.domain.event.PaymentCompletedEvent;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.event.PaymentFailedEvent;

import java.util.List;

public interface PaymentDomainService {
    
    PaymentEvent initiatePayment(Payment payment, Credit credit, List<String> failureMessages);
    
    PaymentCancelledEvent cancelPayment(Payment payment, Credit credit, List<String> failureMessages);
}

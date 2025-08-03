package com.commerce.platform.payment.service.domain;

import com.commerce.platform.payment.service.domain.entity.Credit;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.event.PaymentCancelledEvent;
import com.commerce.platform.payment.service.domain.event.PaymentCompletedEvent;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.event.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
public class PaymentDomainServiceImpl implements PaymentDomainService {
    
    @Override
    public PaymentEvent initiatePayment(Payment payment, Credit credit, List<String> failureMessages) {
        payment.validatePayment();
        payment.initializePayment();
        
        if (credit.getAmount().isGreaterThanOrEqualTo(payment.getPrice())) {
            log.info("Customer has sufficient credit balance for payment initiation");
            credit.subtractAmount(payment.getPrice());
            payment.completePayment();
            
            log.info("Payment with id: {} is completed successfully", payment.getId().getValue());
            
            return new PaymentCompletedEvent(payment, ZonedDateTime.now(ZoneOffset.UTC), List.of());
        } else {
            log.warn("Customer with id: {} doesn't have enough credit balance for payment!", 
                    payment.getCustomerId().getValue());
            payment.failPayment(failureMessages);
            failureMessages.add("Customer with id=" + payment.getCustomerId().getValue() + 
                    " doesn't have enough credit balance for payment!");
            
            log.info("Payment with id: {} is failed", payment.getId().getValue());
            
            return new PaymentFailedEvent(payment, ZonedDateTime.now(ZoneOffset.UTC), failureMessages);
        }
    }
    
    @Override
    public PaymentCancelledEvent cancelPayment(Payment payment, Credit credit, List<String> failureMessages) {
        payment.cancelPayment(failureMessages);
        credit.addAmount(payment.getPrice());
        
        log.info("Payment with id: {} is cancelled", payment.getId().getValue());
        
        return new PaymentCancelledEvent(payment, ZonedDateTime.now(ZoneOffset.UTC), failureMessages);
    }
}

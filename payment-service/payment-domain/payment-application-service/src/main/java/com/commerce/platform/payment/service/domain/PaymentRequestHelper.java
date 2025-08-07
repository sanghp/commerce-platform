package com.commerce.platform.payment.service.domain;

import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.entity.Credit;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import com.commerce.platform.payment.service.domain.mapper.PaymentDataMapper;
import com.commerce.platform.payment.service.domain.ports.output.repository.CreditRepository;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class PaymentRequestHelper {
    
    private final PaymentDomainService paymentDomainService;
    private final PaymentDataMapper paymentDataMapper;
    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    
    public PaymentRequestHelper(PaymentDomainService paymentDomainService,
                               PaymentDataMapper paymentDataMapper,
                               PaymentRepository paymentRepository,
                               CreditRepository creditRepository) {
        this.paymentDomainService = paymentDomainService;
        this.paymentDataMapper = paymentDataMapper;
        this.paymentRepository = paymentRepository;
        this.creditRepository = creditRepository;
    }
    
    @Transactional
    public PaymentEvent persistPayment(PaymentRequest paymentRequest) {
        log.info("Received payment request with payment id: {}", paymentRequest.getId());
        Payment payment = paymentDataMapper.paymentRequestToPayment(paymentRequest);
        Credit credit = getCreditForUpdate(payment.getCustomerId());
        List<String> failureMessages = new ArrayList<>();
        
        PaymentEvent paymentEvent = paymentDomainService.initiatePayment(payment, credit, failureMessages);
        
        savePayment(payment);
        
        if (failureMessages.isEmpty()) {
            saveCredit(credit);
        }
        
        return paymentEvent;
    }
    
    @Transactional
    public PaymentEvent persistCancelPayment(PaymentRequest paymentRequest) {
        log.info("Received payment cancel request with payment id: {}", paymentRequest.getId());
        Optional<Payment> paymentResponse = paymentRepository.findByOrderId(
                new OrderId(paymentRequest.getOrderId()));
        
        if (paymentResponse.isEmpty()) {
            log.error("Payment with order id: {} not found", paymentRequest.getOrderId());
            throw new PaymentDomainException("Payment with order id: " + 
                    paymentRequest.getOrderId() + " not found");
        }
        
        Payment payment = paymentResponse.get();
        Credit credit = getCreditForUpdate(payment.getCustomerId());
        
        List<String> failureMessages = new ArrayList<>();
        PaymentEvent paymentEvent = paymentDomainService.cancelPayment(payment, credit, failureMessages);
        
        savePayment(payment);
        saveCredit(credit);
        
        return paymentEvent;
    }
    
    private Credit getCredit(CustomerId customerId) {
        Optional<Credit> credit = creditRepository.findByCustomerId(customerId);
        
        if (credit.isEmpty()) {
            log.error("Credit with customer id: {} not found", customerId.getValue());
            throw new PaymentDomainException("Credit with customer id: " + 
                    customerId.getValue() + " not found");
        }
        
        return credit.get();
    }
    
    private Credit getCreditForUpdate(CustomerId customerId) {
        Optional<Credit> credit = creditRepository.findByCustomerIdForUpdate(customerId);
        
        if (credit.isEmpty()) {
            log.error("Credit with customer id: {} not found", customerId.getValue());
            throw new PaymentDomainException("Credit with customer id: " + 
                    customerId.getValue() + " not found");
        }
        
        return credit.get();
    }
    
    private void savePayment(Payment payment) {
        Payment savedPayment = paymentRepository.save(payment);
        if (savedPayment == null) {
            log.error("Could not save payment!");
            throw new PaymentDomainException("Could not save payment!");
        }
        log.info("Payment is saved with id: {}", savedPayment.getId().getValue());
    }
    
    private void saveCredit(Credit credit) {
        Credit savedCredit = creditRepository.save(credit);
        if (savedCredit == null) {
            log.error("Could not save credit!");
            throw new PaymentDomainException("Could not save credit!");
        }
        log.info("Credit is saved with id: {}", savedCredit.getId().getValue());
    }
}
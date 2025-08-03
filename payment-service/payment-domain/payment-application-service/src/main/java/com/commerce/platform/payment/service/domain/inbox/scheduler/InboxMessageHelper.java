package com.commerce.platform.payment.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.PaymentOrderStatus;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.domain.PaymentRequestHelper;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.mapper.PaymentDataMapper;
import com.commerce.platform.payment.service.domain.outbox.scheduler.PaymentOutboxHelper;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final PaymentInboxRepository paymentInboxRepository;
    private final PaymentRequestHelper paymentRequestHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentDataMapper paymentDataMapper;
    private final ObjectMapper objectMapper;
    
    public InboxMessageHelper(PaymentInboxRepository paymentInboxRepository,
                             PaymentRequestHelper paymentRequestHelper,
                             PaymentOutboxHelper paymentOutboxHelper,
                             PaymentDataMapper paymentDataMapper,
                             ObjectMapper objectMapper) {
        this.paymentInboxRepository = paymentInboxRepository;
        this.paymentRequestHelper = paymentRequestHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.paymentDataMapper = paymentDataMapper;
        this.objectMapper = objectMapper;
    }
    
    public void processInboxMessages(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            if (!processNextMessage()) {
                break;
            }
        }
    }
    
    @Transactional
    public boolean processNextMessage() {
        List<PaymentInboxMessage> messages = paymentInboxRepository
                .findByStatusOrderByReceivedAtWithSkipLock(InboxStatus.RECEIVED, 1);
        
        if (messages.isEmpty()) {
            return false;
        }
        
        PaymentInboxMessage inboxMessage = messages.getFirst();
        ZonedDateTime processedAt = ZonedDateTime.now();
        
        try {
            if (inboxMessage.getType() == ServiceMessageType.PAYMENT_REQUEST) {
                processPaymentRequest(inboxMessage);
            }
            
            inboxMessage.setStatus(InboxStatus.PROCESSED);
            inboxMessage.setProcessedAt(processedAt);
            
            log.info("Successfully processed inbox message: {} for saga: {}", 
                    inboxMessage.getId(), inboxMessage.getSagaId());
            
        } catch (Exception e) {
            log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
            
            inboxMessage.setStatus(InboxStatus.FAILED);
            inboxMessage.setErrorMessage(e.getMessage());
            inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
        }
        
        paymentInboxRepository.save(inboxMessage);
        return true;
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<PaymentInboxMessage> failedMessages = paymentInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus.FAILED, maxRetryCount, batchSize);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            failedMessages.forEach(message -> message.setStatus(InboxStatus.RECEIVED));
            
            paymentInboxRepository.saveAll(failedMessages);
        }
    }
    
    private void processPaymentRequest(PaymentInboxMessage inboxMessage) throws Exception {
        PaymentRequest request = objectMapper.readValue(
                inboxMessage.getPayload(), 
                PaymentRequest.class
        );
        
        PaymentEvent paymentEvent;
        if (request.getPaymentOrderStatus() == PaymentOrderStatus.PENDING) {
            paymentEvent = paymentRequestHelper.persistPayment(request);
        } else if (request.getPaymentOrderStatus() == PaymentOrderStatus.CANCELLED) {
            paymentEvent = paymentRequestHelper.persistCancelPayment(request);
        } else {
            throw new IllegalStateException("Invalid payment order status: " + request.getPaymentOrderStatus());
        }
        
        paymentOutboxHelper.savePaymentOutboxMessage(
                ServiceMessageType.PAYMENT_RESPONSE,
                paymentDataMapper.paymentEventToPaymentResponse(paymentEvent, inboxMessage.getSagaId()),
                OutboxStatus.STARTED,
                inboxMessage.getSagaId()
        );
        
        log.info("Payment processed for order id: {} with status: {}", 
                paymentEvent.getPayment().getOrderId().getValue(),
                paymentEvent.getPayment().getPaymentStatus());
    }
}
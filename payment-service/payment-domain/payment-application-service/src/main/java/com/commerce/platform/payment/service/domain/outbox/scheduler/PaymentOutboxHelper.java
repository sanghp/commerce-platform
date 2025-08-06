package com.commerce.platform.payment.service.domain.outbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.util.UuidGenerator;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxHelper {
    
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional(readOnly = true)
    public List<PaymentOutboxMessage> getPaymentOutboxMessageByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return paymentOutboxRepository.findByOutboxStatus(outboxStatus, limit);
    }
    
    @Transactional(readOnly = true)
    public List<PaymentOutboxMessage> getPaymentOutboxMessageByOutboxStatusAndFetchedAtBefore(
            OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        return paymentOutboxRepository.findByOutboxStatusAndFetchedAtBefore(outboxStatus, fetchedAtBefore, limit);
    }
    
    @Transactional
    public void save(PaymentOutboxMessage paymentOutboxMessage) {
        PaymentOutboxMessage response = paymentOutboxRepository.save(paymentOutboxMessage);
        if (response == null) {
            log.error("Could not save PaymentOutboxMessage with outbox id: {}", paymentOutboxMessage.getId());
            throw new PaymentDomainException("Could not save PaymentOutboxMessage with outbox id: " +
                    paymentOutboxMessage.getId());
        }
        log.info("PaymentOutboxMessage saved with outbox id: {} and type: {}", 
                paymentOutboxMessage.getId(), paymentOutboxMessage.getType());
    }
    
    @Transactional
    public void savePaymentOutboxMessage(ServiceMessageType messageType,
                                        Object eventPayload,
                                        OutboxStatus outboxStatus,
                                        UUID sagaId) {
        save(PaymentOutboxMessage.builder()
                .id(UuidGenerator.generate())
                .messageId(UuidGenerator.generate())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(messageType)
                .payload(createPayload(eventPayload))
                .outboxStatus(outboxStatus)
                .version(0)
                .build());
    }
    
    @Transactional
    public void saveAll(List<PaymentOutboxMessage> messages) {
        paymentOutboxRepository.saveAll(messages);
        log.info("Saved {} PaymentOutboxMessages", messages.size());
    }
    
    @Transactional
    public int deletePaymentOutboxMessageByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return paymentOutboxRepository.deleteByOutboxStatus(outboxStatus, limit);
    }
    
    @Transactional
    public void updateOutboxMessageStatus(UUID messageId, OutboxStatus status) {
        PaymentOutboxMessage message = paymentOutboxRepository.findById(messageId)
                .orElseThrow(() -> new PaymentDomainException("PaymentOutboxMessage not found with id: " + messageId));
        
        message.setOutboxStatus(status);
        if (status == OutboxStatus.COMPLETED || status == OutboxStatus.FAILED) {
            message.setProcessedAt(ZonedDateTime.now());
        }
        
        save(message);
        log.info("PaymentOutboxMessage {} updated with status: {}", messageId, status);
    }
    
    @Transactional
    public List<PaymentOutboxMessage> updateMessagesToProcessing(int batchSize) {
        List<PaymentOutboxMessage> outboxMessages = getPaymentOutboxMessageByOutboxStatus(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now();
            outboxMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.PROCESSING);
                message.setFetchedAt(now);
            });
            saveAll(outboxMessages);
        }
        
        return outboxMessages;
    }

    @Transactional
    public void resetTimedOutMessages(int processingTimeoutMinutes, int batchSize) {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<PaymentOutboxMessage> timedOutMessages = getPaymentOutboxMessageByOutboxStatusAndFetchedAtBefore(
                OutboxStatus.PROCESSING, 
                timeoutThreshold,
                batchSize);
        
        if (!timedOutMessages.isEmpty()) {
            log.warn("Found {} timed out PROCESSING messages, resetting to STARTED", 
                    timedOutMessages.size());
            timedOutMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.STARTED);
                message.setFetchedAt(null);
            });
            saveAll(timedOutMessages);
        }
    }

    private String createPayload(Object eventPayload) {
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            log.error("Could not create payload object", e);
            throw new PaymentDomainException("Could not create payload object", e);
        }
    }
}
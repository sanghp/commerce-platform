package com.commerce.platform.order.service.domain.outbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.commerce.platform.domain.util.UuidGenerator;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxHelper {

    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<OrderOutboxMessage> getOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return orderOutboxRepository.findByOutboxStatus(outboxStatus, limit);
    }
    
    @Transactional(readOnly = true)
    public List<OrderOutboxMessage> getOrderOutboxMessageByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        return orderOutboxRepository.findByOutboxStatusWithSkipLock(outboxStatus, limit);
    }
    
    @Transactional(readOnly = true)
    public List<OrderOutboxMessage> getOrderOutboxMessageByOutboxStatusAndFetchedAtBefore(
            OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        return orderOutboxRepository.findByOutboxStatusAndFetchedAtBefore(outboxStatus, fetchedAtBefore, limit);
    }


    @Transactional
    public void save(OrderOutboxMessage orderOutboxMessage) {
        OrderOutboxMessage response = orderOutboxRepository.save(orderOutboxMessage);
        if (response == null) {
            log.error("Could not save OrderOutboxMessage with outbox id: {}", orderOutboxMessage.getId());
            throw new OrderDomainException("Could not save OrderOutboxMessage with outbox id: " +
                    orderOutboxMessage.getId());
        }
        log.info("OrderOutboxMessage saved with outbox id: {} and type: {}", orderOutboxMessage.getId(), orderOutboxMessage.getType());
    }

    @Transactional
    public void saveOrderOutboxMessage(ServiceMessageType messageType,
                                       Object eventPayload,
                                       OutboxStatus outboxStatus,
                                       UUID sagaId) {
        save(OrderOutboxMessage.builder()
                .id(UuidGenerator.generate())
                .messageId(UuidGenerator.generate())
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(messageType.name())
                .payload(createPayload(eventPayload))
                .outboxStatus(outboxStatus)
                .version(0)
                .build());
    }

    @Transactional
    public void saveAll(List<OrderOutboxMessage> messages) {
        orderOutboxRepository.saveAll(messages);
        log.info("Saved {} OrderOutboxMessages", messages.size());
    }

    @Transactional
    public int deleteOrderOutboxMessageByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return orderOutboxRepository.deleteByOutboxStatus(outboxStatus, limit);
    }
    
    @Transactional
    public void updateOutboxMessageStatus(UUID messageId, OutboxStatus status) {
        OrderOutboxMessage message = orderOutboxRepository.findById(messageId)
                .orElseThrow(() -> new OrderDomainException("OrderOutboxMessage not found with id: " + messageId));
        
        message.setOutboxStatus(status);
        if (status == OutboxStatus.COMPLETED || status == OutboxStatus.FAILED) {
            message.setProcessedAt(ZonedDateTime.now());
        }
        
        save(message);
        log.info("OrderOutboxMessage {} updated with status: {}", messageId, status);
    }

    @Transactional
    public List<OrderOutboxMessage> updateMessagesToProcessing(int batchSize) {
        List<OrderOutboxMessage> outboxMessages = getOrderOutboxMessageByOutboxStatusWithSkipLock(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now();
            List<UUID> ids = outboxMessages.stream()
                    .map(OrderOutboxMessage::getId)
                    .toList();
            
            int updatedCount = orderOutboxRepository.bulkUpdateStatusAndFetchedAt(ids, OutboxStatus.PROCESSING, now);
            log.info("Updated {} messages to PROCESSING status", updatedCount);
            
            outboxMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.PROCESSING);
                message.setFetchedAt(now);
            });
        }
        
        return outboxMessages;
    }

    @Transactional
    public void resetTimedOutMessages(int processingTimeoutMinutes, int batchSize) {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<OrderOutboxMessage> timedOutMessages = getOrderOutboxMessageByOutboxStatusAndFetchedAtBefore(
                OutboxStatus.PROCESSING, 
                timeoutThreshold,
                batchSize);
        
        if (!timedOutMessages.isEmpty()) {
            log.warn("Found {} timed out PROCESSING messages, resetting to STARTED", 
                    timedOutMessages.size());
            
            List<UUID> ids = timedOutMessages.stream()
                    .map(OrderOutboxMessage::getId)
                    .toList();
            
            int updatedCount = orderOutboxRepository.bulkUpdateStatusAndFetchedAt(ids, OutboxStatus.STARTED, null);
            log.info("Reset {} messages to STARTED status", updatedCount);
            
            timedOutMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.STARTED);
                message.setFetchedAt(null);
            });
        }
    }

    private String createPayload(Object eventPayload) {
        try {
            return objectMapper.writeValueAsString(eventPayload);
        } catch (JsonProcessingException e) {
            log.error("Could not create payload object", e);
            throw new OrderDomainException("Could not create payload object", e);
        }
    }
}
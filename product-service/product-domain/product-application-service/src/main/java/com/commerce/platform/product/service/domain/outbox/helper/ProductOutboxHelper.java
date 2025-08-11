package com.commerce.platform.product.service.domain.outbox.helper;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Component
public class ProductOutboxHelper {

    private final ProductOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ProductOutboxHelper(ProductOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProductOutboxMessage> getProductOutboxMessageByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return outboxRepository.findByOutboxStatus(outboxStatus, limit);
    }
    
    @Transactional(readOnly = true)
    public List<ProductOutboxMessage> getProductOutboxMessageByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        return outboxRepository.findByOutboxStatusWithSkipLock(outboxStatus, limit);
    }

    @Transactional
    public void save(ProductOutboxMessage outboxMessage) {
        ProductOutboxMessage response = outboxRepository.save(outboxMessage);
        if (response == null) {
            log.error("Could not save ProductOutboxMessage with outbox id: {}",
                    outboxMessage.getId());
            throw new ProductDomainException("Could not save ProductOutboxMessage with outbox id: " +
                    outboxMessage.getId());
        }
        log.info("ProductOutboxMessage saved with outbox id: {}", outboxMessage.getId());
    }

    
    @Transactional
    public void updateOutboxMessagesStatus(List<ProductOutboxMessage> messages, OutboxStatus newStatus) {
        messages.forEach(message -> message.setOutboxStatus(newStatus));
        outboxRepository.saveAll(messages);
        log.info("Updated {} ProductOutboxMessages to status: {}", messages.size(), newStatus);
    }


    @Transactional
    public void deleteProductOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        outboxRepository.deleteByOutboxStatus(outboxStatus);
    }

    public ProductOutboxMessage createOutboxMessageWithTrace(UUID messageId, UUID sagaId, 
                                                            ServiceMessageType type, 
                                                            ProductReservationResponseEventPayload payload) {
        // Capture current TraceContext
        SpanContext spanContext = Span.current().getSpanContext();
        String traceId = spanContext.isValid() ? spanContext.getTraceId() : null;
        String spanId = spanContext.isValid() ? spanContext.getSpanId() : null;
        
        String payloadJson = createPayload(payload);
        
        return ProductOutboxMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .sagaId(sagaId)
                .createdAt(ZonedDateTime.now())
                .type(type)
                .payload(payloadJson)
                .outboxStatus(OutboxStatus.STARTED)
                .version(0)
                .traceId(traceId)
                .spanId(spanId)
                .build();
    }

    public String createPayload(ProductReservationResponseEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Could not create ProductReservationResponseEventPayload object for order id: {}",
                    payload.getOrderId(), e);
            throw new ProductDomainException("Could not create ProductReservationResponseEventPayload object for order id: " +
                    payload.getOrderId(), e);
        }
    }
    
    @Transactional
    public void updateOutboxMessageStatus(UUID messageId, OutboxStatus status) {
        ProductOutboxMessage message = outboxRepository.findById(messageId)
                .orElseThrow(() -> new ProductDomainException("ProductOutboxMessage not found with id: " + messageId));
        
        message.setOutboxStatus(status);
        if (status == OutboxStatus.COMPLETED || status == OutboxStatus.FAILED) {
            message.setProcessedAt(ZonedDateTime.now());
        }
        
        save(message);
        log.info("ProductOutboxMessage {} updated with status: {}", messageId, status);
    }

    @Transactional
    public List<ProductOutboxMessage> updateMessagesToProcessing(int batchSize) {
        List<ProductOutboxMessage> outboxMessages = getProductOutboxMessageByOutboxStatusWithSkipLock(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now();
            List<UUID> ids = outboxMessages.stream()
                    .map(ProductOutboxMessage::getId)
                    .toList();
            
            int updatedCount = outboxRepository.bulkUpdateStatusAndFetchedAt(ids, OutboxStatus.PROCESSING, now);
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
        List<ProductOutboxMessage> timedOutMessages = getProductOutboxMessageByOutboxStatusAndFetchedAtBefore(
                OutboxStatus.PROCESSING,
                timeoutThreshold,
                batchSize);
        if (!timedOutMessages.isEmpty()) {
            log.warn("Found {} timed out PROCESSING messages, resetting to STARTED",
                    timedOutMessages.size());
            
            List<UUID> ids = timedOutMessages.stream()
                    .map(ProductOutboxMessage::getId)
                    .toList();
            
            int updatedCount = outboxRepository.bulkUpdateStatusAndFetchedAt(ids, OutboxStatus.STARTED, null);
            log.info("Reset {} messages to STARTED status", updatedCount);
            
            timedOutMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.STARTED);
                message.setFetchedAt(null);
            });
        }
    }

    @Transactional(readOnly = true)
    public List<ProductOutboxMessage> getProductOutboxMessageByOutboxStatusAndFetchedAtBefore(
            OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        return outboxRepository.findByOutboxStatusAndFetchedAtBefore(outboxStatus, fetchedAtBefore, limit);
    }
}
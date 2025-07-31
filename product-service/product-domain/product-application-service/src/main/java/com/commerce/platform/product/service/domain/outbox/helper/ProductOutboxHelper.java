package com.commerce.platform.product.service.domain.outbox.helper;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public void updateOutboxMessage(ProductReservationResponseEventPayload payload, ServiceMessageType type, UUID sagaId) {
        ProductOutboxMessage message = outboxRepository.findByTypeAndSagaId(type, sagaId)
                .orElseThrow(() -> new ProductDomainException("Outbox message not found for type " + type + " and saga id: " + sagaId));

        message.setPayload(createPayload(payload));
        save(message);
        log.info("ProductOutboxMessage updated for saga id: {}", sagaId);
    }
    
    @Transactional
    public void updateOutboxMessagesStatus(List<ProductOutboxMessage> messages, OutboxStatus newStatus) {
        messages.forEach(message -> message.setOutboxStatus(newStatus));
        outboxRepository.saveAll(messages);
        log.info("Updated {} ProductOutboxMessages to status: {}", messages.size(), newStatus);
    }

    @Transactional(readOnly = true)
    public boolean isMessageProcessed(ServiceMessageType type, UUID sagaId) {
        return outboxRepository.findByTypeAndSagaId(type, sagaId).isPresent();
    }
    
    @Transactional(readOnly = true)
    public Optional<ProductOutboxMessage> findByTypeAndSagaId(ServiceMessageType type, UUID sagaId) {
        return outboxRepository.findByTypeAndSagaId(type, sagaId);
    }

    @Transactional
    public void deleteProductOutboxMessageByOutboxStatus(OutboxStatus outboxStatus) {
        outboxRepository.deleteByOutboxStatus(outboxStatus);
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
}
package com.commerce.platform.order.service.domain.outbox.scheduler.product;

import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.ProductReservationOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.commerce.platform.domain.valueobject.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.commerce.platform.saga.order.SagaConstants.ORDER_SAGA_NAME;

@Slf4j
@Component
public class ProductReservationOutboxHelper {

    private final ProductReservationOutboxRepository productReservationOutboxRepository;
    private final ObjectMapper objectMapper;

    public ProductReservationOutboxHelper(ProductReservationOutboxRepository productReservationOutboxRepository, ObjectMapper objectMapper) {
        this.productReservationOutboxRepository = productReservationOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<List<ProductReservationOutboxMessage>>
    getProductReservationOutboxMessageByOutboxStatusAndSagaStatus(
            OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        return productReservationOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME,
                outboxStatus,
                sagaStatus);
    }

    @Transactional(readOnly = true)
    public Optional<ProductReservationOutboxMessage>
    getProductReservationOutboxMessageBySagaIdAndSagaStatus(UUID sagaId, SagaStatus... sagaStatus) {
        return productReservationOutboxRepository.findByTypeAndSagaIdAndSagaStatus(ORDER_SAGA_NAME, sagaId, sagaStatus);
    }



    @Transactional
    public void save(ProductReservationOutboxMessage productReservationOutboxMessage) {
        ProductReservationOutboxMessage response = productReservationOutboxRepository.save(productReservationOutboxMessage);
        if (response == null) {
            log.error("Could not save ProductReservationOutboxMessage with outbox id: {}",
                    productReservationOutboxMessage.getId());
            throw new OrderDomainException("Could not save ProductReservationOutboxMessage with outbox id: " +
                    productReservationOutboxMessage.getId());
        }
        log.info("ProductReservationOutboxMessage saved with outbox id: {}", productReservationOutboxMessage.getId());
    }

    @Transactional
    public void saveProductReservationOutboxMessage(ProductReservationEventPayload productReservationEventPayload,
                                                   OrderStatus orderStatus,
                                                   SagaStatus sagaStatus,
                                                   OutboxStatus outboxStatus,
                                                   UUID sagaId) {
        save(ProductReservationOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(productReservationEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(productReservationEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .outboxStatus(outboxStatus)
                .build());
    }

    @Transactional
    public void deleteProductReservationOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                       SagaStatus... sagaStatus) {
        productReservationOutboxRepository.deleteByTypeAndOutboxStatusAndSagaStatus(ORDER_SAGA_NAME, outboxStatus, sagaStatus);
    }

    private String createPayload(ProductReservationEventPayload productReservationEventPayload) {
        try {
            return objectMapper.writeValueAsString(productReservationEventPayload);
        } catch (JsonProcessingException e) {
            log.error("Could not create ProductReservationEventPayload for order id: {}",
                    productReservationEventPayload.getOrderId(), e);
            throw new OrderDomainException("Could not create ProductReservationEventPayload for order id: " +
                    productReservationEventPayload.getOrderId(), e);
        }
    }

}

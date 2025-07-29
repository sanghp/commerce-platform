package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.input.message.listener.product.ProductReservationResponseMessageListener;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Validated
@Service
public class ProductReservationResponseMessageListenerImpl implements ProductReservationResponseMessageListener {

    private final OrderInboxRepository orderInboxRepository;
    private final ObjectMapper objectMapper;

    public ProductReservationResponseMessageListenerImpl(OrderInboxRepository orderInboxRepository,
                                                          ObjectMapper objectMapper) {
        this.orderInboxRepository = orderInboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void handleProductReservationSucceededResponse(ProductReservationResponse productReservationResponse) {
        saveInboxMessage(productReservationResponse);
        log.info("ProductReservationSucceededResponse saved in inbox table for saga id: {}",
                productReservationResponse.getSagaId());
    }

    @Override
    @Transactional
    public void handleProductReservationFailedResponse(ProductReservationResponse productReservationResponse) {
        saveInboxMessage(productReservationResponse);
        log.info("ProductReservationFailedResponse saved in inbox table for saga id: {}",
                productReservationResponse.getSagaId());
    }

    private void saveInboxMessage(ProductReservationResponse productReservationResponse) {
        try {
            OrderInboxMessage orderInboxMessage = OrderInboxMessage.builder()
                    .id(UUID.randomUUID())
                    .sagaId(productReservationResponse.getSagaId())
                    .eventType(ServiceMessageType.PRODUCT_RESERVATION_RESPONSE)
                    .payload(createPayload(productReservationResponse))
                    .status(InboxStatus.RECEIVED)
                    .receivedAt(ZonedDateTime.now(ZoneOffset.UTC))
                    .retryCount(0)
                    .version(0)
                    .build();

            orderInboxRepository.save(orderInboxMessage);
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate inbox message for saga id: {}, skipping", productReservationResponse.getSagaId());
        }
    }

    private String createPayload(ProductReservationResponse productReservationResponse) {
        try {
            return objectMapper.writeValueAsString(productReservationResponse);
        } catch (JsonProcessingException e) {
            log.error("Could not create ProductReservationResponse object for saga id: {}",
                    productReservationResponse.getSagaId(), e);
            throw new OrderDomainException("Could not create ProductReservationResponse object for saga id " +
                    productReservationResponse.getSagaId(), e);
        }
    }
}

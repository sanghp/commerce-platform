package com.commerce.platform.product.service.domain;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import com.commerce.platform.product.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.input.message.listener.ProductReservationRequestListener;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
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
class ProductReservationRequestListenerImpl implements ProductReservationRequestListener {

    private final ProductInboxRepository productInboxRepository;
    private final ObjectMapper objectMapper;

    public ProductReservationRequestListenerImpl(ProductInboxRepository productInboxRepository,
                                                 ObjectMapper objectMapper) {
        this.productInboxRepository = productInboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void reserveOrder(ProductReservationRequest productReservationRequest) {
        saveInboxMessage(productReservationRequest);
        log.info("ProductReservationRequest saved in inbox table for saga id: {}",
                productReservationRequest.getSagaId());
    }
    
    private void saveInboxMessage(ProductReservationRequest productReservationRequest) {
        try {
            ProductInboxMessage productInboxMessage = ProductInboxMessage.builder()
                    .id(UUID.randomUUID())
                    .sagaId(productReservationRequest.getSagaId())
                    .eventType(ServiceMessageType.PRODUCT_RESERVATION_REQUEST)
                    .payload(createPayload(productReservationRequest))
                    .status(InboxStatus.RECEIVED)
                    .receivedAt(ZonedDateTime.now(ZoneOffset.UTC))
                    .retryCount(0)
                    .version(0)
                    .build();

            productInboxRepository.save(productInboxMessage);
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate inbox message for saga id: {}, skipping", productReservationRequest.getSagaId());
        }
    }

    private String createPayload(ProductReservationRequest productReservationRequest) {
        try {
            return objectMapper.writeValueAsString(productReservationRequest);
        } catch (JsonProcessingException e) {
            log.error("Could not create ProductReservationRequest object for saga id: {}",
                    productReservationRequest.getSagaId(), e);
            throw new ProductDomainException("Could not create ProductReservationRequest object for saga id " +
                    productReservationRequest.getSagaId(), e);
        }
    }
}
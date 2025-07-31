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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import com.commerce.platform.domain.util.UuidGenerator;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

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
    public void reserveOrders(List<ProductReservationRequest> productReservationRequests) {
        if (productReservationRequests.isEmpty()) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<ProductInboxMessage> inboxMessages = productReservationRequests.stream()
                .map(request -> createInboxMessage(request, now))
                .toList();
        
        productInboxRepository.saveAll(inboxMessages);
        
        log.info("Saved {} ProductReservationRequest messages to inbox", inboxMessages.size());
    }
    
    private ProductInboxMessage createInboxMessage(ProductReservationRequest productReservationRequest, ZonedDateTime receivedAt) {
        return ProductInboxMessage.builder()
                .id(UuidGenerator.generate())
                .sagaId(productReservationRequest.getSagaId())
                .type(ServiceMessageType.PRODUCT_RESERVATION_REQUEST)
                .payload(createPayload(productReservationRequest))
                .status(InboxStatus.RECEIVED)
                .receivedAt(receivedAt)
                .retryCount(0)
                .build();
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
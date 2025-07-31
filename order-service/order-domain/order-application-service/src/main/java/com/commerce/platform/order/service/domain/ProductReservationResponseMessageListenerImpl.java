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
    public void saveToInbox(List<ProductReservationResponse> productReservationResponses) {
        if (productReservationResponses.isEmpty()) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<OrderInboxMessage> inboxMessages = productReservationResponses.stream()
                .map(response -> createInboxMessage(response, now))
                .toList();
        
        orderInboxRepository.saveAll(inboxMessages);
        
        log.info("Saved {} ProductReservationResponse messages to inbox", inboxMessages.size());
    }

    private OrderInboxMessage createInboxMessage(ProductReservationResponse productReservationResponse, ZonedDateTime receivedAt) {
        return OrderInboxMessage.builder()
                .id(UuidGenerator.generate())
                .sagaId(productReservationResponse.getSagaId())
                .type(ServiceMessageType.PRODUCT_RESERVATION_RESPONSE)
                .payload(createPayload(productReservationResponse))
                .status(InboxStatus.RECEIVED)
                .receivedAt(receivedAt)
                .retryCount(0)
                .build();
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

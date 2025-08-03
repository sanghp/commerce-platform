package com.commerce.platform.order.service.domain;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.util.UuidGenerator;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;
import com.commerce.platform.order.service.domain.exception.OrderDomainException;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.input.message.listener.payment.PaymentResponseMessageListener;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Validated
@Service
public class PaymentResponseMessageListenerImpl implements PaymentResponseMessageListener {

    private final OrderInboxRepository orderInboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentResponseMessageListenerImpl(OrderInboxRepository orderInboxRepository,
                                             ObjectMapper objectMapper) {
        this.orderInboxRepository = orderInboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void saveToInbox(List<PaymentResponse> paymentResponses) {
        if (paymentResponses.isEmpty()) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<OrderInboxMessage> inboxMessages = paymentResponses.stream()
                .map(response -> createInboxMessage(response, now))
                .toList();
        
        orderInboxRepository.saveAll(inboxMessages);
        
        log.info("Saved {} PaymentResponse messages to inbox", inboxMessages.size());
    }

    private OrderInboxMessage createInboxMessage(PaymentResponse paymentResponse, ZonedDateTime receivedAt) {
        return OrderInboxMessage.builder()
                .id(UuidGenerator.generate())
                .messageId(paymentResponse.getId())
                .sagaId(paymentResponse.getSagaId())
                .type(ServiceMessageType.PAYMENT_RESPONSE)
                .payload(createPayload(paymentResponse))
                .status(InboxStatus.RECEIVED)
                .receivedAt(receivedAt)
                .retryCount(0)
                .build();
    }

    private String createPayload(PaymentResponse paymentResponse) {
        try {
            return objectMapper.writeValueAsString(paymentResponse);
        } catch (JsonProcessingException e) {
            log.error("Could not create PaymentResponse object for saga id: {}",
                    paymentResponse.getSagaId(), e);
            throw new OrderDomainException("Could not create PaymentResponse object for saga id " +
                    paymentResponse.getSagaId(), e);
        }
    }
}

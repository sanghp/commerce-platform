package com.commerce.platform.payment.service.domain;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.util.UuidGenerator;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.exception.PaymentDomainException;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.ports.input.message.listener.PaymentRequestMessageListener;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
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
public class PaymentRequestMessageListenerImpl implements PaymentRequestMessageListener {
    
    private final PaymentInboxRepository paymentInboxRepository;
    private final ObjectMapper objectMapper;
    
    public PaymentRequestMessageListenerImpl(PaymentInboxRepository paymentInboxRepository,
                                           ObjectMapper objectMapper) {
        this.paymentInboxRepository = paymentInboxRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public void saveToInbox(List<PaymentRequest> paymentRequests) {
        if (paymentRequests.isEmpty()) {
            return;
        }
        
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        List<PaymentInboxMessage> inboxMessages = paymentRequests.stream()
                .map(request -> createInboxMessage(request, now))
                .toList();
        
        paymentInboxRepository.saveAll(inboxMessages);
        
        log.info("Saved {} PaymentRequest messages to inbox", inboxMessages.size());
    }
    
    private PaymentInboxMessage createInboxMessage(PaymentRequest paymentRequest, ZonedDateTime receivedAt) {
        return PaymentInboxMessage.builder()
                .id(UuidGenerator.generate())
                .messageId(paymentRequest.getId())
                .sagaId(paymentRequest.getSagaId())
                .type(ServiceMessageType.PAYMENT_REQUEST)
                .payload(createPayload(paymentRequest))
                .status(InboxStatus.RECEIVED)
                .receivedAt(receivedAt)
                .retryCount(0)
                .build();
    }
    
    private String createPayload(PaymentRequest paymentRequest) {
        try {
            return objectMapper.writeValueAsString(paymentRequest);
        } catch (JsonProcessingException e) {
            log.error("Could not create PaymentRequest object for saga id: {}",
                    paymentRequest.getSagaId(), e);
            throw new PaymentDomainException("Could not create PaymentRequest object for saga id " +
                    paymentRequest.getSagaId(), e);
        }
    }
}
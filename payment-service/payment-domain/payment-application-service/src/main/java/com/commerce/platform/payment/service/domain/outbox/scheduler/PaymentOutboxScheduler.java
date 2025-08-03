package com.commerce.platform.payment.service.domain.outbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler implements OutboxScheduler {
    
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentResponseMessagePublisher paymentResponseMessagePublisher;
    
    @Value("${payment-service.outbox-scheduler-batch-size:10}")
    private int batchSize;
    
    @Value("${payment-service.outbox-processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;
    
    @Override
    @Scheduled(fixedDelayString = "${payment-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${payment-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        resetTimedOutMessages();
        List<PaymentOutboxMessage> messagesToProcess = updateMessagesToProcessing();
        
        if (!messagesToProcess.isEmpty()) {
            log.info("Processing {} PaymentOutboxMessages with ids: {}",
                    messagesToProcess.size(),
                    messagesToProcess.stream()
                            .map(msg -> msg.getId().toString())
                            .collect(Collectors.joining(",")));
            
            messagesToProcess.forEach(this::publishMessage);
            
            log.info("{} PaymentOutboxMessages processed", messagesToProcess.size());
        }
    }
    
    @Transactional
    public List<PaymentOutboxMessage> updateMessagesToProcessing() {
        List<PaymentOutboxMessage> outboxMessages = paymentOutboxHelper
                .getPaymentOutboxMessageByOutboxStatus(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now();
            outboxMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.PROCESSING);
                message.setFetchedAt(now);
            });
            paymentOutboxHelper.saveAll(outboxMessages);
        }
        
        return outboxMessages;
    }
    
    private void publishMessage(PaymentOutboxMessage outboxMessage) {
        ServiceMessageType messageType = outboxMessage.getType();
        if (Objects.requireNonNull(messageType) == ServiceMessageType.PAYMENT_RESPONSE) {
            paymentResponseMessagePublisher.publish(outboxMessage);
        } else {
            log.warn("Unknown outbox message type: {}", messageType);
        }
    }
    
    @Transactional
    public void resetTimedOutMessages() {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<PaymentOutboxMessage> timedOutMessages = paymentOutboxHelper
                .getPaymentOutboxMessageByOutboxStatusAndFetchedAtBefore(
                        OutboxStatus.PROCESSING,
                        timeoutThreshold,
                        batchSize);
        
        if (!timedOutMessages.isEmpty()) {
            log.warn("Found {} timed out PROCESSING messages, resetting to STARTED",
                    timedOutMessages.size());
            timedOutMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.STARTED);
                message.setFetchedAt(null);
            });
            paymentOutboxHelper.saveAll(timedOutMessages);
        }
    }
}
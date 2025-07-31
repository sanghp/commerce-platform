package com.commerce.platform.order.service.domain.outbox.scheduler;

import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.product.ProductReservationRequestMessagePublisher;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.commerce.platform.domain.event.ServiceMessageType;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;
    private final PaymentRequestMessagePublisher paymentRequestMessagePublisher;
    private final ProductReservationRequestMessagePublisher productReservationRequestMessagePublisher;
    
    @Value("${order-service.outbox-scheduler-batch-size:10}")
    private int batchSize;
    
    @Value("${order-service.outbox-processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;

    @Override
    @Scheduled(fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        resetTimedOutMessages();
        List<OrderOutboxMessage> messagesToProcess = updateMessagesToProcessing();
        
        if (!messagesToProcess.isEmpty()) {
            log.info("Processing {} OrderOutboxMessages with ids: {}",
                    messagesToProcess.size(),
                    messagesToProcess.stream()
                            .map(msg -> msg.getId().toString())
                            .collect(Collectors.joining(",")));
            
            messagesToProcess.forEach(this::publishMessage);
            
            log.info("{} OrderOutboxMessages processed", messagesToProcess.size());
        }
    }
    
    @Transactional
    public List<OrderOutboxMessage> updateMessagesToProcessing() {
        List<OrderOutboxMessage> outboxMessages = orderOutboxHelper
                .getOrderOutboxMessageByOutboxStatus(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            ZonedDateTime now = ZonedDateTime.now();
            outboxMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.PROCESSING);
                message.setFetchedAt(now);
            });
            orderOutboxHelper.saveAll(outboxMessages);
        }
        
        return outboxMessages;
    }
    
    private void publishMessage(OrderOutboxMessage outboxMessage) {
        ServiceMessageType messageType = ServiceMessageType.valueOf(outboxMessage.getType());
        switch (messageType) {
            case PAYMENT_REQUEST:
                paymentRequestMessagePublisher.publish(outboxMessage);
                break;
            case PRODUCT_RESERVATION_REQUEST:
                productReservationRequestMessagePublisher.publish(outboxMessage);
                break;
            default:
                log.warn("Unknown outbox message type: {}", outboxMessage.getType());
        }
    }

    
    @Transactional
    public void resetTimedOutMessages() {
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(processingTimeoutMinutes);
        List<OrderOutboxMessage> timedOutMessages = orderOutboxHelper
                .getOrderOutboxMessageByOutboxStatusAndFetchedAtBefore(
                        OutboxStatus.PROCESSING, 
                        timeoutThreshold,
                        batchSize);
        
        if (!timedOutMessages.isEmpty()) {
            log.warn("Found {} timed out PROCESSING messages, resetting to STARTED", 
                    timedOutMessages.size());
            timedOutMessages.forEach(message -> {
                message.setOutboxStatus(OutboxStatus.STARTED);
                message.setFetchedAt(null);  // Reset fetched_at
            });
            orderOutboxHelper.saveAll(timedOutMessages);
        }
    }
}
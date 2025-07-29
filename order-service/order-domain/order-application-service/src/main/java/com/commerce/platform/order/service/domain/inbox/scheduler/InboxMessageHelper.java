package com.commerce.platform.order.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.commerce.platform.order.service.domain.ProductReservationSaga;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.commerce.platform.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@Service
public class InboxMessageHelper {
    
    private final OrderInboxRepository orderInboxRepository;
    private final ProductReservationSaga productReservationSaga;
    private final ObjectMapper objectMapper;
    
    public InboxMessageHelper(OrderInboxRepository orderInboxRepository,
                              ProductReservationSaga productReservationSaga,
                              ObjectMapper objectMapper) {
        this.orderInboxRepository = orderInboxRepository;
        this.productReservationSaga = productReservationSaga;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public void processInboxMessages(int batchSize) {
        // Fetch and lock messages in the same transaction
        List<OrderInboxMessage> messages = orderInboxRepository
                .findByStatusOrderByReceivedAt(InboxStatus.RECEIVED, batchSize);
        
        if (messages.isEmpty()) {
            return;
        }
        
        log.debug("Processing {} inbox messages", messages.size());
        
        ZonedDateTime processedAt = ZonedDateTime.now();
        List<OrderInboxMessage> messagesToUpdate = new ArrayList<>();
        
        // First, mark all messages as PROCESSING
        messages.forEach(message -> {
            message.setStatus(InboxStatus.PROCESSING);
            messagesToUpdate.add(message);
        });
        orderInboxRepository.saveAll(messagesToUpdate);
        messagesToUpdate.clear();
        
        // Process each message
        for (OrderInboxMessage inboxMessage : messages) {
            try {
                if (inboxMessage.getEventType() == ServiceMessageType.PRODUCT_RESERVATION_RESPONSE) {
                    processProductReservationResponse(inboxMessage);
                }
                
                inboxMessage.setStatus(InboxStatus.PROCESSED);
                inboxMessage.setProcessedAt(processedAt);
                messagesToUpdate.add(inboxMessage);
                
                log.info("Successfully processed inbox message: {} for saga: {}", 
                        inboxMessage.getId(), inboxMessage.getSagaId());
                
            } catch (Exception e) {
                log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
                
                inboxMessage.setStatus(InboxStatus.FAILED);
                inboxMessage.setErrorMessage(e.getMessage());
                inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
                messagesToUpdate.add(inboxMessage);
            }
        }
        
        // Bulk update all messages
        if (!messagesToUpdate.isEmpty()) {
            orderInboxRepository.saveAll(messagesToUpdate);
        }
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<OrderInboxMessage> failedMessages = orderInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAt(InboxStatus.FAILED, maxRetryCount, batchSize);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            // Reset all messages to RECEIVED status
            failedMessages.forEach(message -> message.setStatus(InboxStatus.RECEIVED));
            
            orderInboxRepository.saveAll(failedMessages);
        }
    }
    
    private void processProductReservationResponse(OrderInboxMessage inboxMessage) throws Exception {
        ProductReservationResponse response = objectMapper.readValue(
                inboxMessage.getPayload(), 
                ProductReservationResponse.class
        );
        
        if (response.getProductReservationStatus() == ProductReservationStatus.APPROVED ||
            response.getProductReservationStatus() == ProductReservationStatus.BOOKED) {
            productReservationSaga.process(response);
            log.info("Order is approved for order id: {}", response.getOrderId());
        } else {
            productReservationSaga.rollback(response);
            log.info("Product Reservation Saga rollback operation is completed for order id: {} with failure messages: {}",
                    response.getOrderId(),
                    String.join(FAILURE_MESSAGE_DELIMITER, response.getFailureMessages()));
        }
    }
}
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
    
    public void processInboxMessages(int batchSize) {
        for (int i = 0; i < batchSize; i++) {
            if (!processNextMessage()) {
                break;
            }
        }
    }
    
    @Transactional
    public boolean processNextMessage() {
        List<OrderInboxMessage> messages = orderInboxRepository
                .findByStatusOrderByReceivedAtWithSkipLock(InboxStatus.RECEIVED, 1);
        
        if (messages.isEmpty()) {
            return false;
        }
        
        OrderInboxMessage inboxMessage = messages.getFirst();
        ZonedDateTime processedAt = ZonedDateTime.now();
        
        try {
            if (inboxMessage.getType() == ServiceMessageType.PRODUCT_RESERVATION_RESPONSE) {
                processProductReservationResponse(inboxMessage);
            }
            
            inboxMessage.setStatus(InboxStatus.PROCESSED);
            inboxMessage.setProcessedAt(processedAt);
            
            log.info("Successfully processed inbox message: {} for saga: {}", 
                    inboxMessage.getId(), inboxMessage.getSagaId());
            
        } catch (Exception e) {
            log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
            
            inboxMessage.setStatus(InboxStatus.FAILED);
            inboxMessage.setErrorMessage(e.getMessage());
            inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
        }
        
        orderInboxRepository.save(inboxMessage);
        return true;
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<OrderInboxMessage> failedMessages = orderInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus.FAILED, maxRetryCount, batchSize);
        
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
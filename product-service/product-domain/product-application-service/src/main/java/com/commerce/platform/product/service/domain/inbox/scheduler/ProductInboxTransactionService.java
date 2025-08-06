package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
public class ProductInboxTransactionService {
    
    private final ProductInboxRepository productInboxRepository;
    private final ProductInboxMessageProcessor productInboxMessageProcessor;
    
    public ProductInboxTransactionService(ProductInboxRepository productInboxRepository,
                                        ProductInboxMessageProcessor productInboxMessageProcessor) {
        this.productInboxRepository = productInboxRepository;
        this.productInboxMessageProcessor = productInboxMessageProcessor;
    }
    
    @Transactional
    public boolean processNextMessage() {
        List<ProductInboxMessage> messages = productInboxRepository
                .findByStatusOrderByReceivedAtWithSkipLock(InboxStatus.RECEIVED, 1);
        
        if (messages.isEmpty()) {
            return false;
        }
        
        ProductInboxMessage inboxMessage = messages.getFirst();
        ZonedDateTime processedAt = ZonedDateTime.now();
        
        try {
            if (inboxMessage.getType() == ServiceMessageType.PRODUCT_RESERVATION_REQUEST) {
                productInboxMessageProcessor.processProductReservationRequest(inboxMessage);
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
        
        productInboxRepository.save(inboxMessage);
        return true;
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        List<ProductInboxMessage> failedMessages = productInboxRepository
                .findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus.FAILED, maxRetryCount, batchSize);
        
        if (!failedMessages.isEmpty()) {
            log.info("Retrying {} failed messages", failedMessages.size());
            
            failedMessages.forEach(message -> message.setStatus(InboxStatus.RECEIVED));
            
            productInboxRepository.saveAll(failedMessages);
        }
    }
} 
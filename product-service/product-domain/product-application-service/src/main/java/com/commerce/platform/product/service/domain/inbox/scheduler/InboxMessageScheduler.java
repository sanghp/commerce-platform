package com.commerce.platform.product.service.domain.inbox.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboxMessageScheduler {
    
    @Value("${product-service.inbox-max-retry-count:3}")
    private int maxRetryCount;
    
    @Value("${product-service.inbox-batch-size:100}")
    private int batchSize;
    
    private final InboxMessageHelper inboxMessageHelper;
    
    public InboxMessageScheduler(InboxMessageHelper inboxMessageHelper) {
        this.inboxMessageHelper = inboxMessageHelper;
    }
    
    @Scheduled(fixedDelayString = "${product-service.inbox-scheduler-fixed-rate:100}")
    public void processInboxMessages() {
        try {
            for (int i = 0; i < batchSize; i++) {
                if (!inboxMessageHelper.processNextMessage()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Failed to process inbox messages", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${product-service.inbox-retry-scheduler-fixed-rate:3000}")
    public void retryFailedMessages() {
        try {
            inboxMessageHelper.retryFailedMessages(maxRetryCount, batchSize);
        } catch (Exception e) {
            log.error("Failed to retry failed messages", e);
        }
    }
}
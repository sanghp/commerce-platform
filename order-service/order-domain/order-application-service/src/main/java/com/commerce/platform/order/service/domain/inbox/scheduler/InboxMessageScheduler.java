package com.commerce.platform.order.service.domain.inbox.scheduler;

import com.commerce.platform.inbox.InboxScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InboxMessageScheduler implements InboxScheduler {
    
    @Value("${order-service.inbox-max-retry-count:3}")
    private int maxRetryCount;
    
    @Value("${order-service.inbox-batch-size:100}")
    private int batchSize;
    
    private final InboxMessageHelper inboxMessageHelper;
    
    public InboxMessageScheduler(InboxMessageHelper inboxMessageHelper) {
        this.inboxMessageHelper = inboxMessageHelper;
    }
    
    @Override
    @Scheduled(fixedDelayString = "${order-service.inbox-scheduler-fixed-rate:100}")
    public void processInboxMessages() {
        try {
            inboxMessageHelper.processInboxMessages(batchSize);
        } catch (Exception e) {
            log.error("Failed to process inbox messages", e);
        }
    }
    
    @Scheduled(fixedDelayString = "${order-service.inbox-retry-scheduler-fixed-rate:3000}")
    public void retryFailedMessages() {
        try {
            inboxMessageHelper.retryFailedMessages(maxRetryCount, batchSize);
        } catch (Exception e) {
            log.error("Failed to retry failed messages", e);
        }
    }
}
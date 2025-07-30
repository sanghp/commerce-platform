package com.commerce.platform.product.service.domain.inbox.scheduler;

import com.commerce.platform.product.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InboxMessageScheduler {
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 100;
    
    private final InboxMessageHelper inboxMessageHelper;
    
    public InboxMessageScheduler(InboxMessageHelper inboxMessageHelper) {
        this.inboxMessageHelper = inboxMessageHelper;
    }
    
    @Scheduled(fixedDelay = 100)
    public void processInboxMessages() {
        try {
            inboxMessageHelper.processInboxMessages(BATCH_SIZE);
        } catch (Exception e) {
            log.error("Failed to process inbox messages", e);
        }
    }
    
    @Scheduled(fixedDelay = 3000)
    public void retryFailedMessages() {
        try {
            inboxMessageHelper.retryFailedMessages(MAX_RETRY_COUNT, BATCH_SIZE);
        } catch (Exception e) {
            log.error("Failed to retry failed messages", e);
        }
    }
}
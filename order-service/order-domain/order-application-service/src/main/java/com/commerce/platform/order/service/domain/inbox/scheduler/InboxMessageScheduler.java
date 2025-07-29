package com.commerce.platform.order.service.domain.inbox.scheduler;

import com.commerce.platform.order.service.domain.inbox.model.InboxStatus;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderInboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InboxMessageScheduler {
    
    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 100;
    
    private final OrderInboxRepository orderInboxRepository;
    private final InboxMessageHelper inboxMessageHelper;
    
    public InboxMessageScheduler(OrderInboxRepository orderInboxRepository,
                                 InboxMessageHelper inboxMessageHelper) {
        this.orderInboxRepository = orderInboxRepository;
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
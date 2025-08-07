package com.commerce.platform.payment.service.domain.inbox.scheduler;

import com.commerce.platform.inbox.InboxScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInboxScheduler implements InboxScheduler {
    
    private final InboxMessageHelper inboxMessageHelper;
    
    @Value("${payment-service.inbox-scheduler-batch-size:10}")
    private int batchSize;
    
    @Value("${payment-service.inbox-max-retry-count:3}")
    private int maxRetryCount;
    
    @Override
    @Scheduled(fixedRateString = "${payment-service.inbox-scheduler-fixed-rate}",
            initialDelayString = "${payment-service.inbox-scheduler-initial-delay}")
    @Async("inboxTaskExecutor")
    public void processInboxMessages() {
        log.debug("Processing payment inbox messages...");
        inboxMessageHelper.processInboxMessages(batchSize);
    }
    
    @Scheduled(fixedRateString = "${payment-service.inbox-retry-scheduler-fixed-rate}",
            initialDelayString = "${payment-service.inbox-retry-scheduler-initial-delay}")
    @Async("inboxTaskExecutor")
    public void retryFailedMessages() {
        log.debug("Retrying failed payment inbox messages...");
        inboxMessageHelper.retryFailedMessages(maxRetryCount, batchSize);
    }
}
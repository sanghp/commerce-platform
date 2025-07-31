package com.commerce.platform.order.service.domain.outbox.scheduler;

import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxCleanerScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;
    
    @Value("${order-service.outbox-cleaner-batch-size:100}")
    private int batchSize;

    @Override
    @Scheduled(cron = "${order-service.outbox-cleaner-cron:@midnight}")
    public void processOutboxMessage() {
        log.info("Starting to clean up completed outbox messages");
        
        int totalDeleted = 0;
        int deleted;
        
        do {
            deleted = orderOutboxHelper.deleteOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED, batchSize);
            totalDeleted += deleted;
            
            if (deleted > 0) {
                log.debug("Deleted {} completed outbox messages in this batch", deleted);
            }
        } while (deleted == batchSize);
        
        log.info("Completed outbox messages cleaned up. Total deleted: {}", totalDeleted);
    }
}
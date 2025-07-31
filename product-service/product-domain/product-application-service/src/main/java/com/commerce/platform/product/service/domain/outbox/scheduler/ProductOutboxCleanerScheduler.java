package com.commerce.platform.product.service.domain.outbox.scheduler;

import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ProductOutboxCleanerScheduler implements OutboxScheduler {

    private final ProductOutboxHelper outboxHelper;
    
    @Value("${product-service.outbox-cleaner-batch-size:100}")
    private int batchSize;

    public ProductOutboxCleanerScheduler(ProductOutboxHelper outboxHelper) {
        this.outboxHelper = outboxHelper;
    }

    @Override
    @Scheduled(cron = "${product-service.outbox-cleaner-cron:@midnight}")
    public void processOutboxMessage() {
        List<ProductOutboxMessage> outboxMessages = 
                outboxHelper.getProductOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED, batchSize);
        if (!outboxMessages.isEmpty()) {
            log.info("Received {} ProductOutboxMessage for clean-up.", outboxMessages.size());
            outboxHelper.deleteProductOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);
            log.info("{} ProductOutboxMessage deleted!", outboxMessages.size());
        }
    }
} 
package com.commerce.platform.product.service.domain.outbox.scheduler;

import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductOutboxCleanerScheduler implements OutboxScheduler {

    private final ProductOutboxHelper outboxHelper;

    public ProductOutboxCleanerScheduler(ProductOutboxHelper outboxHelper) {
        this.outboxHelper = outboxHelper;
    }

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<ProductOutboxMessage>> outboxMessagesResponse =
                outboxHelper.getProductOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);
        if (outboxMessagesResponse.isPresent()) {
            List<ProductOutboxMessage> outboxMessages = outboxMessagesResponse.get();
            log.info("Received {} ProductOutboxMessage for clean-up.", outboxMessages.size());
            outboxHelper.deleteProductOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);
            log.info("{} ProductOutboxMessage deleted!", outboxMessages.size());
        }

    }
} 
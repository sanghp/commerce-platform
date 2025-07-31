package com.commerce.platform.product.service.domain.outbox.scheduler;

import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.message.publisher.ProductReservationResponseMessagePublisher;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductOutboxScheduler implements OutboxScheduler {

    private final ProductOutboxHelper outboxHelper;
    private final ProductReservationResponseMessagePublisher responseMessagePublisher;
    
    @Value("${product-service.outbox-scheduler-batch-size:10}")
    private int batchSize;

    public ProductOutboxScheduler(ProductOutboxHelper outboxHelper,
                                             ProductReservationResponseMessagePublisher responseMessagePublisher) {
        this.outboxHelper = outboxHelper;
        this.responseMessagePublisher = responseMessagePublisher;
    }

    @Override
    @Scheduled(fixedDelayString = "${product-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${product-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        // 트랜잭션 내에서 STARTED -> PROCESSING 상태로 업데이트
        List<ProductOutboxMessage> messagesToProcess = updateMessagesToProcessing();
        
        if (!messagesToProcess.isEmpty()) {
            log.info("Processing {} ProductOutboxMessages with ids: {}",
                    messagesToProcess.size(),
                    messagesToProcess.stream()
                            .map(msg -> msg.getId().toString())
                            .collect(Collectors.joining(",")));
            
            // 트랜잭션 밖에서 메시지 발행
            messagesToProcess.forEach(this::publishMessage);
            
            log.info("{} ProductOutboxMessages processed", messagesToProcess.size());
        }
    }
    
    @Transactional
    public List<ProductOutboxMessage> updateMessagesToProcessing() {
        List<ProductOutboxMessage> outboxMessages = outboxHelper
                .getProductOutboxMessageByOutboxStatus(OutboxStatus.STARTED, batchSize);
        
        if (!outboxMessages.isEmpty()) {
            outboxHelper.updateOutboxMessagesStatus(outboxMessages, OutboxStatus.PROCESSING);
        }
        
        return outboxMessages;
    }
    
    private void publishMessage(ProductOutboxMessage outboxMessage) {
        responseMessagePublisher.publish(outboxMessage, this::updateOutboxStatus);
    }

    @Transactional
    public void updateOutboxStatus(ProductOutboxMessage outboxMessage, OutboxStatus outboxStatus) {
        outboxMessage.setOutboxStatus(outboxStatus);
        if (outboxStatus == OutboxStatus.COMPLETED || outboxStatus == OutboxStatus.FAILED) {
            outboxMessage.setProcessedAt(ZonedDateTime.now());
        }
        outboxHelper.save(outboxMessage);
        log.info("ProductOutboxMessage is updated with outbox status: {}", outboxStatus.name());
    }
} 
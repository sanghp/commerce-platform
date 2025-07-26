package com.commerce.platform.product.service.domain.outbox.scheduler;

import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.message.publisher.ProductReservationResponseMessagePublisher;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductOutboxScheduler implements OutboxScheduler {

    private final ProductOutboxHelper outboxHelper;
    private final ProductReservationResponseMessagePublisher responseMessagePublisher;

    public ProductOutboxScheduler(ProductOutboxHelper outboxHelper,
                                             ProductReservationResponseMessagePublisher responseMessagePublisher) {
        this.outboxHelper = outboxHelper;
        this.responseMessagePublisher = responseMessagePublisher;
    }

    @Override
    @Scheduled(fixedDelayString = "${product-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${product-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        Optional<List<ProductOutboxMessage>> outboxMessagesResponse =
                outboxHelper.getProductOutboxMessageByOutboxStatus(OutboxStatus.STARTED);

        if (outboxMessagesResponse.isPresent() && !outboxMessagesResponse.get().isEmpty()) {
            List<ProductOutboxMessage> outboxMessages = outboxMessagesResponse.get();

            log.info("Received {} ProductOutboxMessage with ids: {}, sending to message bus!",
                    outboxMessages.size(),
                    outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(",")));

            outboxMessages.forEach(outboxMessage ->
                    responseMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));

            log.info("{} ProductOutboxMessage sent to message bus!", outboxMessages.size());

        }
    }

    @Transactional
    private void updateOutboxStatus(ProductOutboxMessage outboxMessage, OutboxStatus outboxStatus) {
        outboxMessage.setOutboxStatus(outboxStatus);
        outboxHelper.save(outboxMessage);
        log.info("ProductOutboxMessage is updated with outbox status: {}", outboxStatus.name());
    }
} 
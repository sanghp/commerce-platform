package com.commerce.platform.order.service.domain.outbox.scheduler.product;

import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductReservationCleanerScheduler implements OutboxScheduler {

    private final ProductReservationOutboxHelper reservationOutboxHelper;

    public ProductReservationCleanerScheduler(ProductReservationOutboxHelper reservationOutboxHelper) {
        this.reservationOutboxHelper = reservationOutboxHelper;
    }

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<ProductReservationOutboxMessage>> outboxMessagesResponse =
                reservationOutboxHelper.getProductReservationOutboxMessageByOutboxStatusAndSagaStatus(
                        OutboxStatus.COMPLETED,
                        SagaStatus.SUCCEEDED,
                        SagaStatus.FAILED,
                        SagaStatus.COMPENSATED);
        if (outboxMessagesResponse.isPresent()) {
            List<ProductReservationOutboxMessage> outboxMessages = outboxMessagesResponse.get();
            log.info("Received {} ProductReservationOutboxMessage for clean-up. The payloads: {}",
                    outboxMessages.size(),
                    outboxMessages.stream().map(ProductReservationOutboxMessage::getPayload)
                            .collect(Collectors.joining("\n")));
            reservationOutboxHelper.deleteProductReservationOutboxMessageByOutboxStatusAndSagaStatus(
                    OutboxStatus.COMPLETED,
                    SagaStatus.SUCCEEDED,
                    SagaStatus.FAILED,
                    SagaStatus.COMPENSATED);
            log.info("{} ProductReservationOutboxMessage deleted!", outboxMessages.size());
        }

    }
}

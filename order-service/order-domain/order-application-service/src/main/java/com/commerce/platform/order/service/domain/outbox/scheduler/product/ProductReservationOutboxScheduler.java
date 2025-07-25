package com.commerce.platform.order.service.domain.outbox.scheduler.product;

import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.product.ProductReservationMessagePublisher;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductReservationOutboxScheduler implements OutboxScheduler {

    private final ProductReservationOutboxHelper reservationOutboxHelper;
    private final ProductReservationMessagePublisher reservationMessagePublisher;

    public ProductReservationOutboxScheduler(ProductReservationOutboxHelper reservationOutboxHelper,
                                             ProductReservationMessagePublisher reservationMessagePublisher) {
        this.reservationOutboxHelper = reservationOutboxHelper;
        this.reservationMessagePublisher = reservationMessagePublisher;
    }

    @Override
    @Scheduled(fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        Optional<List<ProductReservationOutboxMessage>> outboxMessagesResponse =
                reservationOutboxHelper.getProductReservationOutboxMessageByOutboxStatusAndSagaStatus(
                        OutboxStatus.STARTED,
                        SagaStatus.STARTED, SagaStatus.PROCESSING);

        if (outboxMessagesResponse.isPresent() && !outboxMessagesResponse.get().isEmpty()) {
            List<ProductReservationOutboxMessage> outboxMessages = outboxMessagesResponse.get();

            log.info("Received {} ProductReservationOutboxMessage with ids: {}, sending to message bus!",
                    outboxMessages.size(),
                    outboxMessages.stream().map(outboxMessage ->
                            outboxMessage.getId().toString()).collect(Collectors.joining(",")));

            outboxMessages.forEach(outboxMessage ->
                    reservationMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));

            log.info("{} ProductReservationOutboxMessage sent to message bus!", outboxMessages.size());

        }
    }

    @Transactional
    private void updateOutboxStatus(ProductReservationOutboxMessage reservationOutboxMessage, OutboxStatus outboxStatus) {
        reservationOutboxMessage.setOutboxStatus(outboxStatus);
        reservationOutboxHelper.save(reservationOutboxMessage);
        log.info("ProductReservationOutboxMessage is updated with outbox status: {}", outboxStatus.name());
    }
}

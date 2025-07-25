package com.commerce.platform.order.service.domain.outbox.model.product;

import com.commerce.platform.domain.valueobject.OrderStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationOutboxMessage {
    private final UUID id;
    private final UUID sagaId;
    private final ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
    private final String type;
    private final String payload;
    private SagaStatus sagaStatus;
    private final OrderStatus orderStatus;
    private OutboxStatus outboxStatus;
    private final int version;

    public void setProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public void setSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }

    public void setOutboxStatus(OutboxStatus outboxStatus) {
        this.outboxStatus = outboxStatus;
    }
}
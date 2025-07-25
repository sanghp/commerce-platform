package com.commerce.platform.order.service.domain.outbox.model.product;

import com.commerce.platform.domain.valueobject.OrderStatus;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationOutboxMessage {
    private final UUID id;
    private final UUID sagaId;
    private final ZonedDateTime createdAt;
    @Setter
    private ZonedDateTime processedAt;
    private final String type;
    private final String payload;
    @Setter
    private SagaStatus sagaStatus;
    private final OrderStatus orderStatus;
    @Setter
    private OutboxStatus outboxStatus;
    private final int version;

}
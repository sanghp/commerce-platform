package com.commerce.platform.product.service.domain.outbox.model;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductOutboxMessage {
    private final UUID id;
    private final UUID messageId;
    private final UUID sagaId;
    private final ZonedDateTime createdAt;
    @Setter
    private ZonedDateTime fetchedAt;
    @Setter
    private ZonedDateTime processedAt;
    private final ServiceMessageType type;
    @Setter
    private String payload;
    @Setter
    private OutboxStatus outboxStatus;
    private int version;

}
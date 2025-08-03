package com.commerce.platform.order.service.domain.outbox.model;

import com.commerce.platform.outbox.OutboxStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrderOutboxMessage {
    private UUID id;
    private UUID messageId;
    private UUID sagaId;
    private ZonedDateTime createdAt;
    private ZonedDateTime fetchedAt;
    private ZonedDateTime processedAt;
    private String type;
    private String payload;
    private OutboxStatus outboxStatus;
    private int version;

    public void setProcessedAt(ZonedDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public void setOutboxStatus(OutboxStatus outboxStatus) {
        this.outboxStatus = outboxStatus;
    }
    
    public void setFetchedAt(ZonedDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
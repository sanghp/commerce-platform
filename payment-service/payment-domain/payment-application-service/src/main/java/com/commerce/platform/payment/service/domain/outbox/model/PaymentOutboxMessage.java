package com.commerce.platform.payment.service.domain.outbox.model;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PaymentOutboxMessage {
    private UUID id;
    private UUID messageId;
    private UUID sagaId;
    private ZonedDateTime createdAt;
    @Setter
    private ZonedDateTime fetchedAt;
    @Setter
    private ZonedDateTime processedAt;
    private ServiceMessageType type;
    private String payload;
    @Setter
    private OutboxStatus outboxStatus;
    private int version;
    private String traceId;
    private String spanId;

}
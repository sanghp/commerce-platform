package com.commerce.platform.product.service.domain.inbox.model;

import com.commerce.platform.domain.event.ServiceMessageType;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInboxMessage {
    private UUID id;
    private UUID sagaId;
    private ServiceMessageType type;
    private String payload;
    private InboxStatus status;
    private ZonedDateTime receivedAt;
    private ZonedDateTime processedAt;
    private int retryCount;
    private String errorMessage;
    private int version;
}
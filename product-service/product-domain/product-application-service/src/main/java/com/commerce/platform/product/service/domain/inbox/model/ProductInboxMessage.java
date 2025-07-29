package com.commerce.platform.product.service.domain.inbox.model;

import com.commerce.platform.domain.event.ServiceMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductInboxMessage {
    private final UUID id;
    private final UUID sagaId;
    private final ServiceMessageType eventType;
    private final String payload;
    private final ZonedDateTime processedAt;
} 
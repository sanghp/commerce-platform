package com.commerce.platform.product.service.domain.inbox.model;

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
    private final String eventType;
    private final String payload;
    private final ZonedDateTime processedAt;
} 
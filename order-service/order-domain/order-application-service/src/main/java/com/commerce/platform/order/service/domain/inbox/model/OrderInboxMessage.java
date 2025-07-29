package com.commerce.platform.order.service.domain.inbox.model;

import com.commerce.platform.domain.event.ServiceMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OrderInboxMessage {
    private final UUID id;
    private final UUID sagaId;
    private final ServiceMessageType eventType;
    private final String payload;
    private InboxStatus status;
    private final ZonedDateTime receivedAt;
    private ZonedDateTime processedAt;
    private Integer retryCount;
    private String errorMessage;
    private Integer version;
}
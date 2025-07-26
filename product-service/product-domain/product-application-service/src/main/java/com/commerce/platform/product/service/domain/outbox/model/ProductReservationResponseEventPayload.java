package com.commerce.platform.product.service.domain.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationResponseEventPayload {
    private final UUID orderId;
    private final UUID sagaId;
    private final String reservationStatus;
    private final List<String> failureMessages;
    private final ZonedDateTime createdAt;
    private final List<ProductReservationProduct> products;
} 
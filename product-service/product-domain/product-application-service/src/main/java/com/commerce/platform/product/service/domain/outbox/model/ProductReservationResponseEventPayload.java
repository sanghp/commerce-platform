package com.commerce.platform.product.service.domain.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReservationResponseEventPayload {
    private UUID orderId;
    private UUID sagaId;
    private String reservationStatus;
    private List<String> failureMessages;
    private ZonedDateTime createdAt;
    private List<ProductReservationProduct> products;
} 
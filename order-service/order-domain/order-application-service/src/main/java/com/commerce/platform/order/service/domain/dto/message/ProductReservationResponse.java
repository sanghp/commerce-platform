package com.commerce.platform.order.service.domain.dto.message;

import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationResponse {
    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private Instant createdAt;
    private ProductReservationStatus productReservationStatus;
    private List<ReservedOrderItem> products;
    private List<String> failureMessages;
}


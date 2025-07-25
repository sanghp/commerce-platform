package com.commerce.platform.order.service.domain.outbox.model.product;

import com.commerce.platform.domain.valueobject.ProductReservationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationEventPayload {
    @JsonProperty
    private UUID orderId;
    @JsonProperty
    private UUID customerId;
    @JsonProperty
    private BigDecimal price;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private ProductReservationOrderStatus reservationOrderStatus;
    @JsonProperty
    private List<ProductReservationEventProduct> products;
}

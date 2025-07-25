package com.commerce.platform.order.service.domain.outbox.model.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationEventProduct {
    @JsonProperty
    private UUID id;
    @JsonProperty
    private Integer quantity;
}
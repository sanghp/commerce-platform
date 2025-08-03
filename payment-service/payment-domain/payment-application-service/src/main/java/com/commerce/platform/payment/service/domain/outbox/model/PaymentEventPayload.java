package com.commerce.platform.payment.service.domain.outbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventPayload {

    @JsonProperty
    private UUID id;
    @JsonProperty
    private UUID sagaId;
    @JsonProperty
    private UUID paymentId;
    @JsonProperty
    private UUID orderId;
    @JsonProperty
    private UUID customerId;
    @JsonProperty
    private BigDecimal price;
    @JsonProperty
    private ZonedDateTime createdAt;
    @JsonProperty
    private String paymentStatus;
    @JsonProperty
    private List<String> failureMessages;
}
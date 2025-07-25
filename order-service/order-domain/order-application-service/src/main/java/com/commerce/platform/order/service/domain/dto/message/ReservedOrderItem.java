package com.commerce.platform.order.service.domain.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ReservedOrderItem {
    private UUID productId;
    private int quantity;
} 
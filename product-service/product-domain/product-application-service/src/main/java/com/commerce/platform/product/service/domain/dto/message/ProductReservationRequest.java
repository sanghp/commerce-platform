package com.commerce.platform.product.service.domain.dto.message;

import com.commerce.platform.product.service.domain.entity.Product;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProductReservationRequest {
    private UUID id;
    private UUID sagaId;
    private UUID orderId;
    private ProductReservationOrderStatus reservationOrderStatus;
    private List<Product> products;
    private BigDecimal price;
    private Instant createdAt;
} 
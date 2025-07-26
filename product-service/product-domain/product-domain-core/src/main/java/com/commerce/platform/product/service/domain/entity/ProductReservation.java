package com.commerce.platform.product.service.domain.entity;

import com.commerce.platform.domain.entity.AggregateRoot;
import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationId;
import com.commerce.platform.product.service.domain.valueobject.SagaId;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class ProductReservation extends AggregateRoot<ProductReservationId> {
    private final ProductId productId;
    private final OrderId orderId;
    private final SagaId sagaId;
    private final Integer quantity;
    private ProductReservationStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @Builder
    public ProductReservation(ProductReservationId productReservationId,
                             ProductId productId,
                             OrderId orderId,
                             SagaId sagaId,
                             Integer quantity) {
        super.setId(productReservationId);
        this.productId = productId;
        this.orderId = orderId;
        this.sagaId = sagaId;
        this.quantity = quantity;
        this.status = ProductReservationStatus.PENDING;
    }

    public void initializeReservation(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void confirm() {
        this.status = ProductReservationStatus.CONFIRMED;
        this.updatedAt = ZonedDateTime.now();
    }

    public void cancel() {
        this.status = ProductReservationStatus.CANCELLED;
        this.updatedAt = ZonedDateTime.now();
    }

    public void validateReservation() {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (sagaId == null) {
            throw new IllegalArgumentException("Saga ID cannot be null");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }
} 
package com.commerce.platform.product.service.domain.entity;

import com.commerce.platform.domain.entity.AggregateRoot;
import com.commerce.platform.domain.valueobject.Money;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.domain.exception.ProductDomainException;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
public class Product extends AggregateRoot<ProductId> {
    @Setter
    private String name;
    private final Money price;
    @Setter
    private Integer quantity;
    @Setter
    private Integer reservedQuantity;
    private final boolean enabled;
    @Setter
    private ZonedDateTime createdAt;

    @Builder
    public Product(ProductId productId,
                   String name,
                   Money price,
                   Integer quantity,
                   Integer reservedQuantity,
                   boolean enabled,
                   ZonedDateTime createdAt) {
        super.setId(productId);
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public void validateProduct() {
        validateName();
        validatePrice();
        validateQuantity();
        validateReservedQuantity();
        validateCreatedAt();
    }

    private void validateName() {
        if (name == null || name.trim().isEmpty()) {
            throw new ProductDomainException("Product name cannot be null or empty");
        }
        if (name.length() > 255) {
            throw new ProductDomainException("Product name cannot exceed 255 characters");
        }
    }

    private void validatePrice() {
        if (price == null || !price.isGreaterThanZero()) {
            throw new ProductDomainException("Product price must be greater than zero");
        }
    }

    private void validateQuantity() {
        if (quantity == null) {
            throw new ProductDomainException("Product quantity cannot be null");
        }
        if (quantity < 0) {
            throw new ProductDomainException("Product quantity cannot be negative");
        }
    }

    private void validateReservedQuantity() {
        if (reservedQuantity == null) {
            throw new ProductDomainException("Product reserved quantity cannot be null");
        }
        if (reservedQuantity < 0) {
            throw new ProductDomainException("Product reserved quantity cannot be negative");
        }
    }

    private void validateCreatedAt() {
        if (createdAt == null) {
            throw new ProductDomainException("Product created at cannot be null");
        }
    }

    public void validateAndReserveProduct(Integer requestedQuantity) {
        validateQuantity();
        validateReservedQuantity();

        if (requestedQuantity == null) {
            throw new ProductDomainException("Requested quantity cannot be null");
        }
        if (requestedQuantity <= 0) {
            throw new ProductDomainException("Requested quantity must be greater than zero");
        }
        
        int availableQuantity = quantity - reservedQuantity;

        if (availableQuantity < requestedQuantity) {
            throw new ProductDomainException("Insufficient product quantity. Available: " + availableQuantity + ", Requested: " + requestedQuantity);
        }
        
        this.reservedQuantity += requestedQuantity;
    }

    public void validateAndCancelReservation(Integer cancelledQuantity) {
        validateReservedQuantity();
        if (cancelledQuantity == null) {
            throw new ProductDomainException("Cancelled quantity cannot be null");
        }
        if (cancelledQuantity <= 0) {
            throw new ProductDomainException("Cancelled quantity must be greater than zero");
        }
        
        if (reservedQuantity < cancelledQuantity) {
            throw new ProductDomainException("Cannot cancel more than reserved quantity. Reserved: " + reservedQuantity + ", Cancelled: " + cancelledQuantity);
        }
        
        this.reservedQuantity -= cancelledQuantity;
    }

    public void confirmReservation(Integer confirmedQuantity) {
        validateQuantity();
        validateReservedQuantity();
        if (confirmedQuantity == null) {
            throw new ProductDomainException("Confirmed quantity cannot be null");
        }
        if (confirmedQuantity <= 0) {
            throw new ProductDomainException("Confirmed quantity must be greater than zero");
        }
        
        if (reservedQuantity < confirmedQuantity) {
            throw new ProductDomainException("Cannot confirm more than reserved quantity. Reserved: " + reservedQuantity + ", Confirmed: " + confirmedQuantity);
        }
        
        if (quantity < confirmedQuantity) {
            throw new ProductDomainException("Cannot confirm more than available quantity. Available: " + quantity + ", Confirmed: " + confirmedQuantity);
        }
        
        this.quantity -= confirmedQuantity;
        this.reservedQuantity -= confirmedQuantity;
    }

    public void restoreReservedQuantity(Integer restoredQuantity) {
        validateReservedQuantity();
        if (restoredQuantity == null) {
            throw new ProductDomainException("Restored quantity cannot be null");
        }
        if (restoredQuantity <= 0) {
            throw new ProductDomainException("Restored quantity must be greater than zero");
        }
        
        if (reservedQuantity < restoredQuantity) {
            throw new ProductDomainException("Cannot restore more than reserved quantity. Reserved: " + reservedQuantity + ", Restored: " + restoredQuantity);
        }
        
        this.reservedQuantity -= restoredQuantity;
    }

}
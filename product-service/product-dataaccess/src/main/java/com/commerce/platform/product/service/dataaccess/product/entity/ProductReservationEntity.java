package com.commerce.platform.product.service.dataaccess.product.entity;

import com.commerce.platform.product.service.domain.valueobject.ProductReservationStatus;
import lombok.*;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_reservations")
@Entity
public class ProductReservationEntity {

    @Id
    private UUID id;
    private UUID productId;
    private UUID orderId;
    private Integer quantity; 
    @Enumerated(EnumType.STRING)
    private ProductReservationStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductReservationEntity that = (ProductReservationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 
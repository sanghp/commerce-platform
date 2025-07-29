package com.commerce.platform.product.service.dataaccess.inbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_inbox")
@Entity
public class ProductInboxEntity {

    @Id
    private UUID id;
    private UUID sagaId;
    private String eventType;
    private String payload;
    private ZonedDateTime processedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductInboxEntity that = (ProductInboxEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 
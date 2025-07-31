package com.commerce.platform.order.service.dataaccess.inbox.entity;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.order.service.domain.inbox.model.InboxStatus;
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
@Table(name = "order_inbox")
@Entity
public class OrderInboxEntity {

    @Id
    private UUID id;
    private UUID sagaId;
    @Enumerated(EnumType.STRING)
    private ServiceMessageType type;
    private String payload;
    @Enumerated(EnumType.STRING)
    private InboxStatus status;
    private ZonedDateTime receivedAt;
    private ZonedDateTime processedAt;
    private Integer retryCount;
    private String errorMessage;
    
    @Version
    private Integer version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderInboxEntity that = (OrderInboxEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
package com.commerce.platform.payment.service.dataaccess.inbox.entity;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
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
@Table(name = "payment_inbox")
@Entity
public class PaymentInboxEntity {
    
    @Id
    private UUID id;
    
    private UUID messageId;
    
    private UUID sagaId;
    
    @Enumerated(EnumType.STRING)
    private ServiceMessageType type;
    
    private String payload;
    
    @Enumerated(EnumType.STRING)
    private InboxStatus status;
    
    private ZonedDateTime receivedAt;
    
    private ZonedDateTime processedAt;
    
    private int retryCount;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private String traceId;
    
    private String spanId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentInboxEntity that = (PaymentInboxEntity) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
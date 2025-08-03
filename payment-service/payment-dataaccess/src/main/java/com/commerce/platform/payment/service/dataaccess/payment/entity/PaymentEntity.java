package com.commerce.platform.payment.service.dataaccess.payment.entity;

import com.commerce.platform.domain.valueobject.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments")
@Entity
public class PaymentEntity {
    
    @Id
    private UUID id;
    
    private UUID orderId;
    
    private UUID customerId;
    
    private BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    private ZonedDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String failureMessages;
}
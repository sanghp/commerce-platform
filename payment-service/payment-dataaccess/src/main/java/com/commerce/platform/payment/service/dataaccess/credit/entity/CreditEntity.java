package com.commerce.platform.payment.service.dataaccess.credit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "credits")
@Entity
public class CreditEntity {
    
    @Id
    private UUID id;
    
    private UUID customerId;
    
    private BigDecimal amount;
}
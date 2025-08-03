package com.commerce.platform.payment.service.domain.ports.output.repository;

import com.commerce.platform.domain.valueobject.CustomerId;
import com.commerce.platform.payment.service.domain.entity.Credit;

import java.util.Optional;

public interface CreditRepository {
    
    Credit save(Credit credit);
    
    Optional<Credit> findByCustomerId(CustomerId customerId);
}
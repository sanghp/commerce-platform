package com.commerce.platform.payment.service.dataaccess.credit.repository;

import com.commerce.platform.payment.service.dataaccess.credit.entity.CreditEntity;

import java.util.Optional;
import java.util.UUID;

public interface CreditCustomRepository {
    Optional<CreditEntity> findByCustomerIdForUpdate(UUID customerId);
}
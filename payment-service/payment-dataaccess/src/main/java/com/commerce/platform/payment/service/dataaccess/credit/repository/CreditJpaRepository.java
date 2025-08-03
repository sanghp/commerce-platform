package com.commerce.platform.payment.service.dataaccess.credit.repository;

import com.commerce.platform.payment.service.dataaccess.credit.entity.CreditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditJpaRepository extends JpaRepository<CreditEntity, UUID> {
    
    Optional<CreditEntity> findByCustomerId(UUID customerId);
}
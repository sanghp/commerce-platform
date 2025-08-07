package com.commerce.platform.payment.service.dataaccess.credit.repository;

import com.commerce.platform.payment.service.dataaccess.credit.entity.CreditEntity;
import com.commerce.platform.payment.service.dataaccess.credit.entity.QCreditEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class CreditCustomRepositoryImpl implements CreditCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QCreditEntity credit = QCreditEntity.creditEntity;

    public CreditCustomRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Optional<CreditEntity> findByCustomerIdForUpdate(UUID customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        
        CreditEntity creditEntity = queryFactory
                .selectFrom(credit)
                .where(credit.customerId.eq(customerId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        log.debug("Found credit with lock for customer ID: {}", customerId);

        return Optional.ofNullable(creditEntity);
    }
}
package com.commerce.platform.product.service.dataaccess.product.repository;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductEntity;
import com.commerce.platform.product.service.dataaccess.product.entity.QProductEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class ProductCustomRepositoryImpl implements ProductCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final QProductEntity product = QProductEntity.productEntity;

    public ProductCustomRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ProductEntity> findByIdsForUpdate(List<UUID> productIds) {
        List<ProductEntity> products = queryFactory
                .selectFrom(product)
                .where(product.id.in(productIds))
                .orderBy(product.id.asc())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();

        log.info("Found {} products with X-lock for {} requested product IDs", 
                products.size(), productIds.size());

        return products;
    }
} 
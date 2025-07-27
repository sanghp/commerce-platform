package com.commerce.platform.product.service.dataaccess.product.adapter;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.dataaccess.product.entity.ProductReservationEntity;
import com.commerce.platform.product.service.dataaccess.product.mapper.ProductReservationDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.product.repository.ProductReservationJpaRepository;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductReservationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.commerce.platform.product.service.domain.valueobject.SagaId;

@Slf4j
@Component
public class ProductReservationRepositoryImpl implements ProductReservationRepository {

    private final ProductReservationJpaRepository productReservationJpaRepository;
    private final ProductReservationDataAccessMapper productReservationDataAccessMapper;

    public ProductReservationRepositoryImpl(ProductReservationJpaRepository productReservationJpaRepository,
                                            ProductReservationDataAccessMapper productReservationDataAccessMapper) {
        this.productReservationJpaRepository = productReservationJpaRepository;
        this.productReservationDataAccessMapper = productReservationDataAccessMapper;
    }

    @Override
    public ProductReservation save(ProductReservation productReservation) {
        ProductReservationEntity productReservationEntity = productReservationDataAccessMapper.productReservationToProductReservationEntity(productReservation);
        ProductReservationEntity savedEntity = productReservationJpaRepository.save(productReservationEntity);
        log.info("Product reservation is saved with id: {}", savedEntity.getId());
        return productReservationDataAccessMapper.productReservationEntityToProductReservation(savedEntity);
    }

    @Override
    public List<ProductReservation> saveAll(List<ProductReservation> productReservations) {
        List<ProductReservationEntity> productReservationEntities = productReservations.stream()
                .map(productReservationDataAccessMapper::productReservationToProductReservationEntity)
                .toList();
        List<ProductReservationEntity> savedEntities = productReservationJpaRepository.saveAll(productReservationEntities);
        log.info("{} product reservations are saved", savedEntities.size());
        return productReservationDataAccessMapper.productReservationEntitiesToProductReservations(savedEntities);
    }

    @Override
    public Optional<ProductReservation> findById(UUID reservationId) {
        return productReservationJpaRepository.findById(reservationId)
                .map(productReservationDataAccessMapper::productReservationEntityToProductReservation);
    }

    @Override
    public List<ProductReservation> findByOrderId(OrderId orderId) {
        List<ProductReservationEntity> productReservationEntities = productReservationJpaRepository.findByOrderId(orderId.getValue());
        return productReservationDataAccessMapper.productReservationEntitiesToProductReservations(productReservationEntities);
    }

    @Override
    public List<ProductReservation> findBySagaId(SagaId sagaId) {
        List<ProductReservationEntity> productReservationEntities = productReservationJpaRepository.findBySagaId(sagaId.getValue());
        return productReservationDataAccessMapper.productReservationEntitiesToProductReservations(productReservationEntities);
    }

    @Override
    public List<ProductReservation> findByProductId(ProductId productId) {
        List<ProductReservationEntity> productReservationEntities = productReservationJpaRepository.findByProductId(productId.getValue());
        return productReservationDataAccessMapper.productReservationEntitiesToProductReservations(productReservationEntities);
    }

    @Override
    public void deleteById(UUID reservationId) {
        productReservationJpaRepository.deleteById(reservationId);
        log.info("Product reservation is deleted with id: {}", reservationId);
    }
} 
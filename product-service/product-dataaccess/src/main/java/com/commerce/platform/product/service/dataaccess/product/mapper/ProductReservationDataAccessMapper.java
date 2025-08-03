package com.commerce.platform.product.service.dataaccess.product.mapper;

import com.commerce.platform.product.service.dataaccess.product.entity.ProductReservationEntity;
import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.product.service.domain.valueobject.ProductReservationId;
import org.springframework.stereotype.Component;

import java.util.List;

import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.domain.valueobject.OrderId;

@Component
public class ProductReservationDataAccessMapper {

    public ProductReservationEntity productReservationToProductReservationEntity(ProductReservation productReservation) {
        return ProductReservationEntity.builder()
                .id(productReservation.getId().getValue())
                .productId(productReservation.getProductId().getValue())
                .orderId(productReservation.getOrderId().getValue())
                .quantity(productReservation.getQuantity())
                .status(productReservation.getStatus())
                .createdAt(productReservation.getCreatedAt())
                .updatedAt(productReservation.getUpdatedAt())
                .build();
    }

    public ProductReservation productReservationEntityToProductReservation(ProductReservationEntity productReservationEntity) {
        return ProductReservation.builder()
                .productReservationId(new ProductReservationId(productReservationEntity.getId()))
                .productId(new ProductId(productReservationEntity.getProductId()))
                .orderId(new OrderId(productReservationEntity.getOrderId()))
                .quantity(productReservationEntity.getQuantity())
                .status(productReservationEntity.getStatus())
                .createdAt(productReservationEntity.getCreatedAt())
                .updatedAt(productReservationEntity.getUpdatedAt())
                .build();
    }

    public List<ProductReservation> productReservationEntitiesToProductReservations(List<ProductReservationEntity> productReservationEntities) {
        return productReservationEntities.stream()
                .map(this::productReservationEntityToProductReservation)
                .toList();
    }
} 
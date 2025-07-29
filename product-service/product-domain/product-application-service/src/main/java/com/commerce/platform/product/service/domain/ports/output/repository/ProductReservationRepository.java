package com.commerce.platform.product.service.domain.ports.output.repository;

import com.commerce.platform.domain.valueobject.OrderId;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.product.service.domain.entity.ProductReservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReservationRepository {

    ProductReservation save(ProductReservation productReservation);

    List<ProductReservation> saveAll(List<ProductReservation> productReservations);

    Optional<ProductReservation> findById(UUID reservationId);

    List<ProductReservation> findByOrderId(OrderId orderId);

    List<ProductReservation> findByProductId(ProductId productId);

    void deleteById(UUID reservationId);
} 
package com.commerce.platform.product.service.domain;

import com.commerce.platform.product.service.domain.entity.ProductReservation;
import com.commerce.platform.domain.valueobject.ProductId;
import com.commerce.platform.domain.valueobject.OrderId;

import java.time.ZonedDateTime;

public interface ProductReservationDomainService {

    ProductReservation createProductReservation(ProductId productId, OrderId orderId,
                                               Integer quantity, ZonedDateTime requestTime);

    ProductReservation confirmProductReservation(ProductReservation reservation, ZonedDateTime requestTime);

    ProductReservation cancelProductReservation(ProductReservation reservation, ZonedDateTime requestTime);
} 
package com.commerce.platform.product.service.domain.ports.input.message.listener;

import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;

public interface ProductReservationRequestListener {
    void reserveOrder(ProductReservationRequest productReservationRequest);
}

package com.commerce.platform.order.service.domain.ports.input.message.listener.product;

import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;

import java.util.List;

public interface ProductReservationResponseMessageListener {
    void handleProductReservationResponses(List<ProductReservationResponse> productReservationResponses);
}

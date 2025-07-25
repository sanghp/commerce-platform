package com.commerce.platform.order.service.domain.ports.input.message.listener.product;

import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;

public interface ProductReservationResponseMessageListener {
    void handleProductReservationSucceededResponse(ProductReservationResponse productReservationResponse);

    void handleProductReservationFailedResponse(ProductReservationResponse productReservationResponse);
}

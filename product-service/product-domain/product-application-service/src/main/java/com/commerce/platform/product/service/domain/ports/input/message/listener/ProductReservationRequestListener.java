package com.commerce.platform.product.service.domain.ports.input.message.listener;

import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;

import java.util.List;

public interface ProductReservationRequestListener {
    void saveToInbox(List<ProductReservationRequest> productReservationRequests);
    void saveToInboxWithTrace(List<ProductReservationRequest> productReservationRequests, String traceId, String spanId);
}

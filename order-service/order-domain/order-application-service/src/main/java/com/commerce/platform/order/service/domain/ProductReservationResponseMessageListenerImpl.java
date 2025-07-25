package com.commerce.platform.order.service.domain;

import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.ports.input.message.listener.product.ProductReservationResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static com.commerce.platform.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@Validated
@Service
public class ProductReservationResponseMessageListenerImpl implements ProductReservationResponseMessageListener {

    private final ProductReservationSaga productReservationSaga;

    public ProductReservationResponseMessageListenerImpl(ProductReservationSaga productReservationSaga) {
        this.productReservationSaga = productReservationSaga;
    }

    @Override
    public void handleProductReservationSucceededResponse(ProductReservationResponse productReservationResponse) {
        productReservationSaga.process(productReservationResponse);
        log.info("Order is approved for order id: {}", productReservationResponse.getOrderId());
    }

    @Override
    public void handleProductReservationFailedResponse(ProductReservationResponse productReservationResponse) {
        productReservationSaga.rollback(productReservationResponse);
        log.info("Product Reservation Saga rollback operation is completed for order id: {} with failure messages: {}",
                productReservationResponse.getOrderId(),
                String.join(FAILURE_MESSAGE_DELIMITER, productReservationResponse.getFailureMessages()));
    }
}

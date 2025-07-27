package com.commerce.platform.order.service.messaging.listener.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationStatus;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.order.service.domain.exception.OrderNotFoundException;
import com.commerce.platform.order.service.domain.ports.input.message.listener.product.ProductReservationResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.commerce.platform.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@Component
public class ProductReservationResponseKafkaListener implements KafkaConsumer<ProductReservationResponseAvroModel> {

    private final ProductReservationResponseMessageListener reservationResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;

    public ProductReservationResponseKafkaListener(ProductReservationResponseMessageListener
                                                           reservationResponseMessageListener,
                                                   OrderMessagingDataMapper orderMessagingDataMapper) {
        this.reservationResponseMessageListener = reservationResponseMessageListener;
        this.orderMessagingDataMapper = orderMessagingDataMapper;
    }

    @Override
    @KafkaListener(groupId = "${kafka-consumer-config.product-reservation-consumer-group-id}",
            topics = "${order-service.product-reservation-response-topic-name}")
    public void receive(@Payload List<ProductReservationResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{} number of product reservation responses received with keys {}, partitions {} and offsets {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        messages.forEach(productReservationResponseAvroModel -> {
            try {
                if (
                    ProductReservationStatus.APPROVED == productReservationResponseAvroModel.getProductReservationStatus() ||
                    ProductReservationStatus.BOOKED == productReservationResponseAvroModel.getProductReservationStatus()
                ) {
                    log.info("Processing approved order for order id: {}",
                            productReservationResponseAvroModel.getOrderId());
                    reservationResponseMessageListener.handleProductReservationSucceededResponse(orderMessagingDataMapper
                            .productReservationResponseAvroModelToProductReservationResponse(productReservationResponseAvroModel));
                } else if (
                    ProductReservationStatus.REJECTED == productReservationResponseAvroModel.getProductReservationStatus() ||
                    ProductReservationStatus.CANCELLED == productReservationResponseAvroModel.getProductReservationStatus()
                ) {
                    log.info("Processing rejected order for order id: {}, with failure messages: {}",
                            productReservationResponseAvroModel.getOrderId(),
                            String.join(FAILURE_MESSAGE_DELIMITER,
                                    productReservationResponseAvroModel.getFailureMessages()));
                    reservationResponseMessageListener.handleProductReservationFailedResponse(orderMessagingDataMapper
                            .productReservationResponseAvroModelToProductReservationResponse(productReservationResponseAvroModel));
                }
            } catch (OptimisticLockingFailureException e) {
                log.error("Caught optimistic locking exception in ProductReservationResponseKafkaListener for order id: {}",
                        productReservationResponseAvroModel.getOrderId());
            } catch (OrderNotFoundException e) {
                log.error("No order found for order id: {}", productReservationResponseAvroModel.getOrderId());
            }
        });

    }
}

package com.commerce.platform.order.service.messaging.listener.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.ports.input.message.listener.product.ProductReservationResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

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

        try {
            List<ProductReservationResponse> responses = messages.stream()
                    .map(orderMessagingDataMapper::productReservationResponseAvroModelToProductReservationResponse)
                    .toList();
                    
            reservationResponseMessageListener.handleProductReservationResponses(responses);
            
        } catch (Exception e) {
            log.error("Failed to process product reservation responses", e);
        }

    }
}

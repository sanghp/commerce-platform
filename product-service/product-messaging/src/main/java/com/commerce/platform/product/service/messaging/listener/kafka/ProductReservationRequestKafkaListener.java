package com.commerce.platform.product.service.messaging.listener.kafka;

import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.product.service.domain.dto.message.ProductReservationRequest;
import com.commerce.platform.product.service.domain.ports.input.message.listener.ProductReservationRequestListener;
import com.commerce.platform.product.service.messaging.mapper.ProductMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ProductReservationRequestKafkaListener implements KafkaConsumer<ProductReservationRequestAvroModel> {
    private final ProductReservationRequestListener productReservationRequestListener;
    private final ProductMessagingDataMapper productMessagingDataMapper;

    public ProductReservationRequestKafkaListener(
            ProductReservationRequestListener productReservationRequestListener,
            ProductMessagingDataMapper productMessagingDataMapper
    ) {
        this.productReservationRequestListener = productReservationRequestListener;
        this.productMessagingDataMapper = productMessagingDataMapper;
    }

    @Override
    @KafkaListener(
            groupId = "${kafka-consumer-config.product-reservation-consumer-group-id}",
            topics = "${product-service.product-reservation-request-topic-name}"
    )
    public void receive(
            @Payload List<ProductReservationRequestAvroModel> messages,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{} number of product reservation requests received with keys {}, partitions {} and offsets {}",
                messages.size(),
                keys.toString(),
                partitions.toString(),
                offsets.toString());

        try {
            List<ProductReservationRequest> requests = messages.stream()
                    .map(productMessagingDataMapper::productReservationRequestAvroModelToProductReservation)
                    .toList();
                    
            productReservationRequestListener.saveToInbox(requests);
            
        } catch (Exception e) {
            log.error("Failed to process product reservation requests", e);
        }
    }
}





































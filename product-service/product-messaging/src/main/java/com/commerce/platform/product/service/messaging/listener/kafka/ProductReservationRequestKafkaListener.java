package com.commerce.platform.product.service.messaging.listener.kafka;

import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

@Slf4j
@Component
public class ProductReservationRequestKafkaListener implements KafkaConsumer<ProductReservationRequestAvroModel> {
    private final ProductReservationRequestListener productReservationRequestListener;
    private final ProductMessagingDataMapper productMessagingDataMapper;
    private final TracingKafkaConsumer tracingKafkaConsumer;

    public ProductReservationRequestKafkaListener(
            ProductReservationRequestListener productReservationRequestListener,
            ProductMessagingDataMapper productMessagingDataMapper,
            TracingKafkaConsumer tracingKafkaConsumer
    ) {
        this.productReservationRequestListener = productReservationRequestListener;
        this.productMessagingDataMapper = productMessagingDataMapper;
        this.tracingKafkaConsumer = tracingKafkaConsumer;
    }

    // Single message processing for proper trace context propagation
    @KafkaListener(
            groupId = "${kafka-consumer-config.product-reservation-consumer-group-id}",
            topics = "${product-service.product-reservation-request-topic-name}"
    )
    public void receiveRecord(ConsumerRecord<String, ProductReservationRequestAvroModel> record) {
        log.info("Product reservation request received with key: {}, partition: {}, offset: {}",
            record.key(), record.partition(), record.offset());
            
        tracingKafkaConsumer.consumeWithTracing(
            record,
            "product-reservation-consumer",
            (value) -> {
                ProductReservationRequest request = productMessagingDataMapper
                    .productReservationRequestAvroModelToProductReservation((ProductReservationRequestAvroModel) value);
                productReservationRequestListener.saveToInbox(List.of(request));
            }
        );
    }
    
    // Keep batch processing for backward compatibility
    @Override
    public void receive(
            @Payload List<ProductReservationRequestAvroModel> messages,
            @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
            @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        // This method is kept for interface compatibility but won't be called
        log.warn("Batch processing called - this should not happen with single message processing enabled");
    }
}





































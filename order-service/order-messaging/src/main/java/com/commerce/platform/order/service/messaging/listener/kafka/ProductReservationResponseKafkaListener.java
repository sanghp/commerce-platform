package com.commerce.platform.order.service.messaging.listener.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.order.service.domain.dto.message.ProductReservationResponse;
import com.commerce.platform.order.service.domain.ports.input.message.listener.product.ProductReservationResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class ProductReservationResponseKafkaListener implements KafkaConsumer<ProductReservationResponseAvroModel> {

    private final ProductReservationResponseMessageListener reservationResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final TracingKafkaConsumer tracingKafkaConsumer;

    public ProductReservationResponseKafkaListener(ProductReservationResponseMessageListener
                                                           reservationResponseMessageListener,
                                                   OrderMessagingDataMapper orderMessagingDataMapper,
                                                   TracingKafkaConsumer tracingKafkaConsumer) {
        this.reservationResponseMessageListener = reservationResponseMessageListener;
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.tracingKafkaConsumer = tracingKafkaConsumer;
    }

    // Single message processing
    @KafkaListener(groupId = "${kafka-consumer-config.product-reservation-consumer-group-id}",
            topics = "${order-service.product-reservation-response-topic-name}")
    public void receiveRecord(ConsumerRecord<String, ProductReservationResponseAvroModel> record) {
        log.info("Product reservation response received with key: {}, partition: {}, offset: {}",
                record.key(), record.partition(), record.offset());

        tracingKafkaConsumer.consumeWithTracing(
            record,
            "product-reservation-consumer",
            (value) -> {
                ProductReservationResponse response = orderMessagingDataMapper
                    .productReservationResponseAvroModelToProductReservationResponse((ProductReservationResponseAvroModel) value);
                reservationResponseMessageListener.saveToInbox(List.of(response));
            }
        );
    }
    
    // Keep for interface compatibility
    @Override
    public void receive(@Payload List<ProductReservationResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        // This method won't be called with batch-listener=false
        log.warn("Batch processing called - this should not happen");
    }
    
    // Batch processing with headers - remove this duplicate listener
    // Use single message processing (receiveRecord) instead for proper trace context handling
}

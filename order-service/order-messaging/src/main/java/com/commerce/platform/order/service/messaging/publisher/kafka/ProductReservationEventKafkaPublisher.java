package com.commerce.platform.order.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import com.commerce.platform.order.service.domain.config.OrderServiceConfigData;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.product.ProductReservationMessagePublisher;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;


import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class ProductReservationEventKafkaPublisher implements ProductReservationMessagePublisher {
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public ProductReservationEventKafkaPublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                                 KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer,
                                                 OrderServiceConfigData orderServiceConfigData,
                                                 KafkaMessageHelper kafkaMessageHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(ProductReservationOutboxMessage productReservationOutboxMessage,
                        BiConsumer<ProductReservationOutboxMessage, OutboxStatus> outboxCallback) {
        var productReservationEventPayload =
                kafkaMessageHelper.getOrderEventPayload(productReservationOutboxMessage.getPayload(),
                        ProductReservationEventPayload.class);

        var sagaId = productReservationOutboxMessage.getSagaId();

        log.info("Received ProductReservationEvent for order id: {} and saga id: {}",
                productReservationEventPayload.getOrderId(),
                sagaId);

        try {
            var productReservationRequestAvroModel = orderMessagingDataMapper
                    .productReservationEventToRequestAvroModel(sagaId, productReservationEventPayload);

            kafkaProducer.send(orderServiceConfigData.getProductReservationRequestTopicName(),
                    sagaId,
                    productReservationRequestAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(
                            orderServiceConfigData.getProductReservationRequestTopicName(),
                            productReservationRequestAvroModel,
                            productReservationOutboxMessage,
                            outboxCallback,
                            productReservationEventPayload.getOrderId(),
                            "ProductReservationRequestAvroModel"));

            log.info("ProductReservationEventPayload sent to Kafka for order id: {} and saga id: {}",
                    productReservationEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending ProductReservationEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    productReservationEventPayload.getOrderId(), sagaId, e.getMessage());
        }

    }
}

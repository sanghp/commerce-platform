package com.commerce.platform.order.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import com.commerce.platform.order.service.domain.config.OrderServiceConfigData;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.product.ProductReservationRequestMessagePublisher;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class ProductReservationRequestKafkaMessagePublisher implements ProductReservationRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public ProductReservationRequestKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                                          KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer,
                                                          OrderServiceConfigData orderServiceConfigData,
                                                          KafkaMessageHelper kafkaMessageHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(OrderOutboxMessage orderOutboxMessage,
                        BiConsumer<OrderOutboxMessage, OutboxStatus> outboxCallback) {
        ProductReservationEventPayload productReservationEventPayload =
                kafkaMessageHelper.getOrderEventPayload(orderOutboxMessage.getPayload(),
                        ProductReservationEventPayload.class);

        UUID sagaId = orderOutboxMessage.getSagaId();

        log.info("Received OrderOutboxMessage for order id: {} and saga id: {}",
                productReservationEventPayload.getOrderId(),
                sagaId);

        try {
            ProductReservationRequestAvroModel productReservationRequestAvroModel = orderMessagingDataMapper
                    .productReservationEventToRequestAvroModel(sagaId, productReservationEventPayload);

            kafkaProducer.send(orderServiceConfigData.getProductReservationRequestTopicName(),
                    sagaId,
                    productReservationRequestAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(orderServiceConfigData.getProductReservationRequestTopicName(),
                            productReservationRequestAvroModel,
                            orderOutboxMessage,
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
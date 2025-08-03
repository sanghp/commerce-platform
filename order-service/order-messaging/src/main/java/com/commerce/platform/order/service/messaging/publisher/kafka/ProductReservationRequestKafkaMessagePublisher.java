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
import com.commerce.platform.order.service.domain.outbox.scheduler.OrderOutboxHelper;

import java.util.UUID;

@Slf4j
@Component
public class ProductReservationRequestKafkaMessagePublisher implements ProductReservationRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderOutboxHelper orderOutboxHelper;

    public ProductReservationRequestKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                                          KafkaProducer<UUID, ProductReservationRequestAvroModel> kafkaProducer,
                                                          OrderServiceConfigData orderServiceConfigData,
                                                          KafkaMessageHelper kafkaMessageHelper,
                                                          OrderOutboxHelper orderOutboxHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.orderOutboxHelper = orderOutboxHelper;
    }

    @Override
    public void publish(OrderOutboxMessage orderOutboxMessage) {
        ProductReservationEventPayload productReservationEventPayload =
                kafkaMessageHelper.getOrderEventPayload(orderOutboxMessage.getPayload(),
                        ProductReservationEventPayload.class);

        UUID sagaId = orderOutboxMessage.getSagaId();

        log.info("Received OrderOutboxMessage for order id: {} and saga id: {}",
                productReservationEventPayload.getOrderId(),
                sagaId);

        try {
            ProductReservationRequestAvroModel productReservationRequestAvroModel = orderMessagingDataMapper
                    .productReservationEventToRequestAvroModel(orderOutboxMessage.getMessageId(), sagaId, productReservationEventPayload);

            kafkaProducer.send(orderServiceConfigData.getProductReservationRequestTopicName(),
                    sagaId,
                    productReservationRequestAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(orderServiceConfigData.getProductReservationRequestTopicName(),
                            productReservationRequestAvroModel,
                            orderOutboxMessage,
                            (message, status) -> {
                                log.info("Kafka callback invoked for message id: {} with status: {}", message.getId(), status);
                                orderOutboxHelper.updateOutboxMessageStatus(message.getId(), status);
                            },
                            productReservationEventPayload.getOrderId(),
                            "ProductReservationRequestAvroModel"));

            log.info("ProductReservationEventPayload sent to Kafka for order id: {} and saga id: {}",
                    productReservationEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending ProductReservationEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    productReservationEventPayload.getOrderId(), sagaId, e.getMessage());
            orderOutboxHelper.updateOutboxMessageStatus(orderOutboxMessage.getId(), OutboxStatus.FAILED);
        }
    }
}
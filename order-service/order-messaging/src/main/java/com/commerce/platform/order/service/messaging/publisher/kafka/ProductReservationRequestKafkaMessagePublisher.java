package com.commerce.platform.order.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import com.commerce.platform.order.service.domain.config.OrderServiceConfigData;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.product.ProductReservationRequestMessagePublisher;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import com.commerce.platform.order.service.domain.outbox.scheduler.OrderOutboxHelper;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class ProductReservationRequestKafkaMessagePublisher implements ProductReservationRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final TracingKafkaProducer<UUID, ProductReservationRequestAvroModel> tracingKafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderOutboxHelper orderOutboxHelper;

    public ProductReservationRequestKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                                          @Qualifier("productReservationTracingKafkaProducer") TracingKafkaProducer<UUID, ProductReservationRequestAvroModel> productReservationTracingKafkaProducer,
                                                          OrderServiceConfigData orderServiceConfigData,
                                                          KafkaMessageHelper kafkaMessageHelper,
                                                          OrderOutboxHelper orderOutboxHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.tracingKafkaProducer = productReservationTracingKafkaProducer;
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

        log.info("Publishing OrderOutboxMessage for order id: {} and saga id: {}",
                productReservationEventPayload.getOrderId(), sagaId);

        try {
            ProductReservationRequestAvroModel productReservationRequestAvroModel = orderMessagingDataMapper
                    .productReservationEventToRequestAvroModel(orderOutboxMessage.getMessageId(), sagaId, productReservationEventPayload);

            // Use explicit trace context if available, otherwise use current context
            CompletableFuture<SendResult<UUID, ProductReservationRequestAvroModel>> sendFuture;
            if (orderOutboxMessage.getTraceId() != null && orderOutboxMessage.getSpanId() != null) {
                sendFuture = tracingKafkaProducer.sendWithTraceContext(
                        orderServiceConfigData.getProductReservationRequestTopicName(),
                        sagaId,
                        productReservationRequestAvroModel,
                        orderOutboxMessage.getTraceId(),
                        orderOutboxMessage.getSpanId());
            } else {
                sendFuture = tracingKafkaProducer.send(
                        orderServiceConfigData.getProductReservationRequestTopicName(),
                        sagaId,
                        productReservationRequestAvroModel);
            }
            
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Kafka callback invoked for message id: {} with status: SUCCESS", orderOutboxMessage.getId());
                    orderOutboxHelper.updateOutboxMessageStatus(orderOutboxMessage.getId(), OutboxStatus.COMPLETED);
                } else {
                    log.error("Failed to send message: {}", ex.getMessage());
                    orderOutboxHelper.updateOutboxMessageStatus(orderOutboxMessage.getId(), OutboxStatus.FAILED);
                }
            });

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
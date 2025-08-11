package com.commerce.platform.order.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import com.commerce.platform.order.service.domain.config.OrderServiceConfigData;
import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
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
public class PaymentRequestKafkaMessagePublisher implements PaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final TracingKafkaProducer<UUID, PaymentRequestAvroModel> tracingKafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final OrderOutboxHelper orderOutboxHelper;

    public PaymentRequestKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                           @Qualifier("paymentRequestTracingKafkaProducer") TracingKafkaProducer<UUID, PaymentRequestAvroModel> paymentRequestTracingKafkaProducer,
                                           OrderServiceConfigData orderServiceConfigData,
                                           KafkaMessageHelper kafkaMessageHelper,
                                           OrderOutboxHelper orderOutboxHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.tracingKafkaProducer = paymentRequestTracingKafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.orderOutboxHelper = orderOutboxHelper;
    }

    @Override
    public void publish(OrderOutboxMessage orderOutboxMessage) {
        OrderPaymentEventPayload orderPaymentEventPayload =
                kafkaMessageHelper.getOrderEventPayload(orderOutboxMessage.getPayload(),
                        OrderPaymentEventPayload.class);

        UUID sagaId = orderOutboxMessage.getSagaId();

        log.info("Publishing OrderOutboxMessage for order id: {} and saga id: {}",
                orderPaymentEventPayload.getOrderId(), sagaId);

        try {
            PaymentRequestAvroModel paymentRequestAvroModel = orderMessagingDataMapper
                    .orderPaymentEventToPaymentRequestAvroModel(orderOutboxMessage.getMessageId(), sagaId, orderPaymentEventPayload);

            // Use explicit trace context if available, otherwise use current context
            CompletableFuture<SendResult<UUID, PaymentRequestAvroModel>> sendFuture;
            if (orderOutboxMessage.getTraceId() != null && orderOutboxMessage.getSpanId() != null) {
                sendFuture = tracingKafkaProducer.sendWithTraceContext(
                        orderServiceConfigData.getPaymentRequestTopicName(),
                        sagaId,
                        paymentRequestAvroModel,
                        orderOutboxMessage.getTraceId(),
                        orderOutboxMessage.getSpanId());
            } else {
                sendFuture = tracingKafkaProducer.send(
                        orderServiceConfigData.getPaymentRequestTopicName(),
                        sagaId,
                        paymentRequestAvroModel);
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

            log.info("OrderPaymentEventPayload sent to Kafka for order id: {} and saga id: {}",
                    orderPaymentEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending OrderPaymentEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    orderPaymentEventPayload.getOrderId(), sagaId, e.getMessage());
            orderOutboxHelper.updateOutboxMessageStatus(orderOutboxMessage.getId(), OutboxStatus.FAILED);
        }
    }
}

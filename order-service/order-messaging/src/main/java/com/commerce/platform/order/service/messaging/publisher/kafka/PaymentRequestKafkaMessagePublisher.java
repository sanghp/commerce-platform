package com.commerce.platform.order.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import com.commerce.platform.order.service.domain.config.OrderServiceConfigData;
import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.commerce.platform.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class PaymentRequestKafkaMessagePublisher implements PaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final KafkaProducer<UUID, PaymentRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;

    public PaymentRequestKafkaMessagePublisher(OrderMessagingDataMapper orderMessagingDataMapper,
                                           KafkaProducer<UUID, PaymentRequestAvroModel> kafkaProducer,
                                           OrderServiceConfigData orderServiceConfigData,
                                           KafkaMessageHelper kafkaMessageHelper) {
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
        this.orderServiceConfigData = orderServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
                        BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback) {
        OrderPaymentEventPayload orderPaymentEventPayload =
                kafkaMessageHelper.getOrderEventPayload(orderPaymentOutboxMessage.getPayload(),
                        OrderPaymentEventPayload.class);

        UUID sagaId = orderPaymentOutboxMessage.getSagaId();

        log.info("Received OrderPaymentOutboxMessage for order id: {} and saga id: {}",
                orderPaymentEventPayload.getOrderId(),
                sagaId);

        try {
            PaymentRequestAvroModel paymentRequestAvroModel = orderMessagingDataMapper
                    .orderPaymentEventToPaymentRequestAvroModel(sagaId, orderPaymentEventPayload);

            kafkaProducer.send(orderServiceConfigData.getPaymentRequestTopicName(),
                    sagaId,
                    paymentRequestAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(orderServiceConfigData.getPaymentRequestTopicName(),
                            paymentRequestAvroModel,
                            orderPaymentOutboxMessage,
                            outboxCallback,
                            orderPaymentEventPayload.getOrderId(),
                            "PaymentRequestAvroModel"));

            log.info("OrderPaymentEventPayload sent to Kafka for order id: {} and saga id: {}",
                    orderPaymentEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending OrderPaymentEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    orderPaymentEventPayload.getOrderId(), sagaId, e.getMessage());
        }


    }
}

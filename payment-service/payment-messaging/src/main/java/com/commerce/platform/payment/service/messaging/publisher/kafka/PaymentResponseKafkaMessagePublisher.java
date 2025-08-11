package com.commerce.platform.payment.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.config.PaymentServiceConfigData;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentEventPayload;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.outbox.scheduler.PaymentOutboxHelper;
import com.commerce.platform.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import com.commerce.platform.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PaymentResponseKafkaMessagePublisher implements PaymentResponseMessagePublisher {

    private final PaymentMessagingDataMapper paymentMessagingDataMapper;
    private final TracingKafkaProducer<UUID, PaymentResponseAvroModel> tracingKafkaProducer;
    private final PaymentServiceConfigData paymentServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;

    public PaymentResponseKafkaMessagePublisher(PaymentMessagingDataMapper paymentMessagingDataMapper,
                                                @Qualifier("paymentResponseTracingKafkaProducer") TracingKafkaProducer<UUID, PaymentResponseAvroModel> paymentResponseTracingKafkaProducer,
                                                PaymentServiceConfigData paymentServiceConfigData,
                                                KafkaMessageHelper kafkaMessageHelper,
                                                PaymentOutboxHelper paymentOutboxHelper) {
        this.paymentMessagingDataMapper = paymentMessagingDataMapper;
        this.tracingKafkaProducer = paymentResponseTracingKafkaProducer;
        this.paymentServiceConfigData = paymentServiceConfigData;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
    }

    @Override
    public void publish(PaymentOutboxMessage paymentOutboxMessage) {
        PaymentEventPayload paymentEventPayload =
                kafkaMessageHelper.getOrderEventPayload(paymentOutboxMessage.getPayload(),
                        PaymentEventPayload.class);

        UUID sagaId = paymentOutboxMessage.getSagaId();

        log.info("Received PaymentOutboxMessage for order id: {} and saga id: {}",
                paymentEventPayload.getOrderId(),
                sagaId);

        try {
            PaymentResponseAvroModel paymentResponseAvroModel = paymentMessagingDataMapper
                    .paymentEventPayloadToPaymentResponseAvroModel(paymentOutboxMessage.getMessageId(), sagaId, paymentEventPayload);

            tracingKafkaProducer.send(paymentServiceConfigData.getPaymentResponseTopicName(),
                    sagaId,
                    paymentResponseAvroModel)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka callback invoked for message id: {} with status: SUCCESS", paymentOutboxMessage.getId());
                            paymentOutboxHelper.updateOutboxMessageStatus(paymentOutboxMessage.getId(), OutboxStatus.COMPLETED);
                        } else {
                            log.error("Failed to send message: {}", ex.getMessage());
                            paymentOutboxHelper.updateOutboxMessageStatus(paymentOutboxMessage.getId(), OutboxStatus.FAILED);
                        }
                    });

            log.info("PaymentEventPayload sent to Kafka for order id: {} and saga id: {}",
                    paymentEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending PaymentEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    paymentEventPayload.getOrderId(), sagaId, e.getMessage());
            paymentOutboxHelper.updateOutboxMessageStatus(paymentOutboxMessage.getId(), OutboxStatus.FAILED);
        }
    }
}
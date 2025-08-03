package com.commerce.platform.payment.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.config.PaymentServiceConfigData;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentEventPayload;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.outbox.scheduler.PaymentOutboxHelper;
import com.commerce.platform.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import com.commerce.platform.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class PaymentResponseKafkaMessagePublisher implements PaymentResponseMessagePublisher {

    private final PaymentMessagingDataMapper paymentMessagingDataMapper;
    private final KafkaProducer<UUID, PaymentResponseAvroModel> kafkaProducer;
    private final PaymentServiceConfigData paymentServiceConfigData;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;

    public PaymentResponseKafkaMessagePublisher(PaymentMessagingDataMapper paymentMessagingDataMapper,
                                                KafkaProducer<UUID, PaymentResponseAvroModel> kafkaProducer,
                                                PaymentServiceConfigData paymentServiceConfigData,
                                                KafkaMessageHelper kafkaMessageHelper,
                                                PaymentOutboxHelper paymentOutboxHelper) {
        this.paymentMessagingDataMapper = paymentMessagingDataMapper;
        this.kafkaProducer = kafkaProducer;
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

            kafkaProducer.send(paymentServiceConfigData.getPaymentResponseTopicName(),
                    sagaId,
                    paymentResponseAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(paymentServiceConfigData.getPaymentResponseTopicName(),
                            paymentResponseAvroModel,
                            paymentOutboxMessage,
                            (message, status) -> {
                                log.info("Kafka callback invoked for message id: {} with status: {}", message.getId(), status);
                                paymentOutboxHelper.updateOutboxMessageStatus(message.getId(), status);
                            },
                            paymentEventPayload.getOrderId(),
                            "PaymentResponseAvroModel"));

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
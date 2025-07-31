package com.commerce.platform.product.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.config.ProductServiceConfigData;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.message.publisher.ProductReservationResponseMessagePublisher;
import com.commerce.platform.product.service.messaging.mapper.ProductMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;

import java.util.UUID;

@Slf4j
@Component
public class ProductReservationEventKafkaPublisher implements ProductReservationResponseMessagePublisher {

    private final ProductServiceConfigData productServiceConfigData;
    private final KafkaProducer<UUID, ProductReservationResponseAvroModel> kafkaProducer;
    private final ProductMessagingDataMapper productMessagingDataMapper;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final ProductOutboxHelper productOutboxHelper;

    public ProductReservationEventKafkaPublisher(ProductServiceConfigData productServiceConfigData,
                                                 KafkaProducer<UUID, ProductReservationResponseAvroModel> kafkaProducer,
                                                 ProductMessagingDataMapper productMessagingDataMapper,
                                                 KafkaMessageHelper kafkaMessageHelper,
                                                 ProductOutboxHelper productOutboxHelper) {
        this.productServiceConfigData = productServiceConfigData;
        this.kafkaProducer = kafkaProducer;
        this.productMessagingDataMapper = productMessagingDataMapper;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.productOutboxHelper = productOutboxHelper;
    }

    @Override
    public void publish(ProductOutboxMessage outboxMessage) {
        var responseEventPayload =
                kafkaMessageHelper.getOrderEventPayload(outboxMessage.getPayload(),
                        ProductReservationResponseEventPayload.class);

        var sagaId = outboxMessage.getSagaId();

        log.info("Received ProductReservationResponseEvent for order id: {} and saga id: {}",
                responseEventPayload.getOrderId(),
                sagaId);

        try {
            var productReservationResponseAvroModel = productMessagingDataMapper
                    .productReservationResponseEventToResponseAvroModel(sagaId, responseEventPayload);

            kafkaProducer.send(productServiceConfigData.getProductReservationResponseTopicName(),
                    sagaId,
                    productReservationResponseAvroModel)
                    .whenComplete(kafkaMessageHelper.getKafkaCallback(
                            productServiceConfigData.getProductReservationResponseTopicName(),
                            productReservationResponseAvroModel,
                            outboxMessage,
                            (message, status) -> {
                                log.info("Kafka callback invoked for message id: {} with status: {}", message.getId(), status);
                                productOutboxHelper.updateOutboxMessageStatus(message.getId(), status);
                            },
                            responseEventPayload.getOrderId(),
                            "ProductReservationResponseAvroModel"));

            log.info("ProductReservationResponseEventPayload sent to Kafka for order id: {} and saga id: {}",
                    responseEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending ProductReservationResponseEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    responseEventPayload.getOrderId(), sagaId, e.getMessage());
            productOutboxHelper.updateOutboxMessageStatus(outboxMessage.getId(), OutboxStatus.FAILED);
        }
    }


} 
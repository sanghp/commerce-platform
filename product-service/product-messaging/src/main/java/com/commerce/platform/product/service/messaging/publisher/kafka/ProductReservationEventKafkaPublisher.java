package com.commerce.platform.product.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationStatus;
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

import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
@Component
public class ProductReservationEventKafkaPublisher implements ProductReservationResponseMessagePublisher {

    private final ProductServiceConfigData productServiceConfigData;
    private final KafkaProducer<UUID, ProductReservationResponseAvroModel> kafkaProducer;
    private final ProductMessagingDataMapper productMessagingDataMapper;
    private final KafkaMessageHelper kafkaMessageHelper;

    public ProductReservationEventKafkaPublisher(ProductServiceConfigData productServiceConfigData,
                                                 KafkaProducer<UUID, ProductReservationResponseAvroModel> kafkaProducer,
                                                 ProductMessagingDataMapper productMessagingDataMapper,
                                                 KafkaMessageHelper kafkaMessageHelper) {
        this.productServiceConfigData = productServiceConfigData;
        this.kafkaProducer = kafkaProducer;
        this.productMessagingDataMapper = productMessagingDataMapper;
        this.kafkaMessageHelper = kafkaMessageHelper;
    }

    @Override
    public void publish(ProductOutboxMessage outboxMessage,
                        BiConsumer<ProductOutboxMessage, OutboxStatus> outboxCallback) {
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
                            outboxCallback,
                            responseEventPayload.getOrderId(),
                            "ProductReservationResponseAvroModel"));

            log.info("ProductReservationResponseEventPayload sent to Kafka for order id: {} and saga id: {}",
                    responseEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending ProductReservationResponseEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    responseEventPayload.getOrderId(), sagaId, e.getMessage());
        }
    }


} 
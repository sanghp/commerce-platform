package com.commerce.platform.order.service.messaging.listener.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.order.service.domain.dto.message.PaymentResponse;
import com.commerce.platform.order.service.domain.ports.input.message.listener.payment.PaymentResponseMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

@Slf4j
@Component
public class PaymentResponseKafkaListener implements KafkaConsumer<PaymentResponseAvroModel> {

    private final PaymentResponseMessageListener paymentResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final TracingKafkaConsumer tracingKafkaConsumer;

    public PaymentResponseKafkaListener(PaymentResponseMessageListener paymentResponseMessageListener,
                                        OrderMessagingDataMapper orderMessagingDataMapper,
                                        TracingKafkaConsumer tracingKafkaConsumer) {
        this.paymentResponseMessageListener = paymentResponseMessageListener;
        this.orderMessagingDataMapper = orderMessagingDataMapper;
        this.tracingKafkaConsumer = tracingKafkaConsumer;
    }

    // Single message processing
    @KafkaListener(groupId = "${kafka-consumer-config.payment-consumer-group-id}", 
                  topics = "${order-service.payment-response-topic-name}")
    public void receiveRecord(ConsumerRecord<String, PaymentResponseAvroModel> record) {
        log.info("Payment response received with key: {}, partition: {}, offset: {}",
                record.key(), record.partition(), record.offset());

        tracingKafkaConsumer.consumeWithTracing(
            record,
            "payment-consumer",
            (value) -> {
                PaymentResponse response = orderMessagingDataMapper
                    .paymentResponseAvroModelToPaymentResponse((PaymentResponseAvroModel) value);
                paymentResponseMessageListener.saveToInbox(List.of(response));
            }
        );
    }
    
    // Keep for interface compatibility
    @Override
    public void receive(@Payload List<PaymentResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        // This method won't be called with batch-listener=false
        log.warn("Batch processing called - this should not happen");
    }
}


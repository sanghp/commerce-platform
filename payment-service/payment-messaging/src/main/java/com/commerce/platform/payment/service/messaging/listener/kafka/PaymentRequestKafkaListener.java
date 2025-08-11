package com.commerce.platform.payment.service.messaging.listener.kafka;

import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.kafka.consumer.KafkaConsumer;
import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.payment.service.domain.ports.input.message.listener.PaymentRequestMessageListener;
import com.commerce.platform.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PaymentRequestKafkaListener implements KafkaConsumer<PaymentRequestAvroModel> {

    private final PaymentRequestMessageListener paymentRequestMessageListener;
    private final PaymentMessagingDataMapper paymentMessagingDataMapper;
    private final TracingKafkaConsumer tracingKafkaConsumer;

    public PaymentRequestKafkaListener(PaymentRequestMessageListener paymentRequestMessageListener,
                                       PaymentMessagingDataMapper paymentMessagingDataMapper,
                                       TracingKafkaConsumer tracingKafkaConsumer) {
        this.paymentRequestMessageListener = paymentRequestMessageListener;
        this.paymentMessagingDataMapper = paymentMessagingDataMapper;
        this.tracingKafkaConsumer = tracingKafkaConsumer;
    }

    // Single message processing for proper trace context propagation
    @KafkaListener(groupId = "${kafka-consumer-config.payment-consumer-group-id}", 
                  topics = "${payment-service.payment-request-topic-name}")
    public void receiveRecord(ConsumerRecord<String, PaymentRequestAvroModel> record) {
        log.info("Payment request received with key: {}, partition: {}, offset: {}",
            record.key(), record.partition(), record.offset());
            
        tracingKafkaConsumer.consumeWithTracing(
            record,
            "payment-consumer",
            (value) -> {
                paymentRequestMessageListener.saveToInbox(
                    List.of(paymentMessagingDataMapper.paymentRequestAvroModelToPaymentRequest((PaymentRequestAvroModel) value))
                );
            }
        );
    }
    
    // Keep batch processing for backward compatibility
    @Override
    public void receive(@Payload List<PaymentRequestAvroModel> messages,
                       @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                       @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        // This method is kept for interface compatibility but won't be called
        log.warn("Batch processing called - this should not happen with single message processing enabled");
    }
}
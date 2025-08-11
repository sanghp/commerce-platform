package com.commerce.platform.order.service.domain;

import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.kafka.order.avro.model.PaymentRequestAvroModel;
import com.commerce.platform.kafka.order.avro.model.ProductReservationRequestAvroModel;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

@Configuration
public class OrderTracingConfig {
    
    @Bean("productReservationTracingKafkaProducer")
    public TracingKafkaProducer<UUID, ProductReservationRequestAvroModel> productReservationTracingKafkaProducer(
            KafkaTemplate<UUID, ProductReservationRequestAvroModel> kafkaTemplate,
            OpenTelemetry openTelemetry) {
        return new TracingKafkaProducer<>(kafkaTemplate, openTelemetry);
    }
    
    @Bean("paymentRequestTracingKafkaProducer")
    public TracingKafkaProducer<UUID, PaymentRequestAvroModel> paymentRequestTracingKafkaProducer(
            KafkaTemplate<UUID, PaymentRequestAvroModel> kafkaTemplate,
            OpenTelemetry openTelemetry) {
        return new TracingKafkaProducer<>(kafkaTemplate, openTelemetry);
    }
    
    @Bean
    public TracingKafkaConsumer tracingKafkaConsumer(OpenTelemetry openTelemetry) {
        return new TracingKafkaConsumer(openTelemetry);
    }
}
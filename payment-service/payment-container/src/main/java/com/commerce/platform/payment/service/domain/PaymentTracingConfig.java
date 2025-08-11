package com.commerce.platform.payment.service.domain;

import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.kafka.order.avro.model.PaymentResponseAvroModel;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

@Configuration
public class PaymentTracingConfig {
    
    @Bean("paymentResponseTracingKafkaProducer")
    public TracingKafkaProducer<UUID, PaymentResponseAvroModel> paymentResponseTracingKafkaProducer(
            KafkaTemplate<UUID, PaymentResponseAvroModel> kafkaTemplate,
            OpenTelemetry openTelemetry) {
        return new TracingKafkaProducer<>(kafkaTemplate, openTelemetry);
    }
    
    @Bean
    public TracingKafkaConsumer tracingKafkaConsumer(OpenTelemetry openTelemetry) {
        return new TracingKafkaConsumer(openTelemetry);
    }
}
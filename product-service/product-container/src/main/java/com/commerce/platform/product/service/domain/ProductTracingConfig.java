package com.commerce.platform.product.service.domain;

import com.commerce.platform.kafka.consumer.service.TracingKafkaConsumer;
import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

@Configuration
public class ProductTracingConfig {
    
    @Bean("productReservationResponseTracingKafkaProducer")
    public TracingKafkaProducer<UUID, ProductReservationResponseAvroModel> productReservationResponseTracingKafkaProducer(
            KafkaTemplate<UUID, ProductReservationResponseAvroModel> kafkaTemplate,
            OpenTelemetry openTelemetry) {
        return new TracingKafkaProducer<>(kafkaTemplate, openTelemetry);
    }
    
    @Bean
    public TracingKafkaConsumer tracingKafkaConsumer(OpenTelemetry openTelemetry) {
        return new TracingKafkaConsumer(openTelemetry);
    }
}
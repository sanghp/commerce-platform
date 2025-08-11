package com.commerce.platform.kafka.producer.service.impl;

import com.commerce.platform.kafka.producer.exception.KafkaProducerException;
import com.commerce.platform.kafka.producer.service.KafkaProducer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaProducerImpl<K extends Serializable, V extends SpecificRecordBase> implements KafkaProducer<K, V> {

    private final KafkaTemplate<K, V> kafkaTemplate;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private static final double SAMPLING_RATE = 0.01; // 1% sampling for Kafka messages
    
    private static final TextMapSetter<Headers> SETTER = (headers, key, value) -> {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    };

    public KafkaProducerImpl(KafkaTemplate<K, V> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.openTelemetry = GlobalOpenTelemetry.get();
        this.tracer = openTelemetry.getTracer("kafka-producer", "1.0.0");
    }

    @Override
    public CompletableFuture<SendResult<K, V>> send(String topicName, K key, V message) {
        ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topicName, key, message);
        return sendWithTracing(producerRecord, topicName, key, message);
    }

    @Override
    public CompletableFuture<SendResult<K, V>> sendWithHeaders(String topicName, K key, V message, Headers headers) {
        ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topicName, null, key, message, headers);
        return sendWithTracing(producerRecord, topicName, key, message);
    }
    
    private CompletableFuture<SendResult<K, V>> sendWithTracing(ProducerRecord<K, V> producerRecord, 
                                                                 String topicName, K key, V message) {
        // Check if current context already has a span (e.g., from PaymentRequestKafkaMessagePublisher)
        Span currentSpan = Span.current();
        boolean hasExistingSpan = currentSpan != null && currentSpan.getSpanContext().isValid();
        
        // Apply sampling - only create new span for 1% of messages if no existing span
        boolean shouldTrace = hasExistingSpan || Math.random() < SAMPLING_RATE;
        
        if (!shouldTrace) {
            // Send without tracing for 99% of messages (when no existing span)
            log.debug("Sending message to topic {} without tracing", topicName);
            return kafkaTemplate.send(producerRecord);
        }
        
        Span span;
        Scope scope = null;
        
        if (hasExistingSpan) {
            // Use existing span context - this preserves the trace from Order->Product->Payment
            span = currentSpan;
            log.debug("Using existing span for topic {} with traceId={}", 
                topicName, span.getSpanContext().getTraceId());
        } else {
            // Create new span only if we don't have one
            span = tracer.spanBuilder("kafka.send " + topicName)
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", topicName)
                .setAttribute("messaging.operation", "send")
                .setAttribute("messaging.kafka.message.key", key != null ? key.toString() : "null")
                .startSpan();
            scope = span.makeCurrent();
        }
        
        try {
            // Always inject trace context into headers for downstream propagation
            W3CTraceContextPropagator.getInstance().inject(
                Context.current(), 
                producerRecord.headers(), 
                SETTER
            );
            
            log.info("Sending message to topic={} with traceId={}, spanId={}", 
                topicName, span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
            
            return kafkaTemplate.send(producerRecord)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        span.setStatus(StatusCode.ERROR, ex.getMessage());
                        span.recordException(ex);
                        log.error("Error on kafka producer with key: {}, message: {} and exception: {}", 
                            key, message, ex.getMessage());
                    } else {
                        RecordMetadata metadata = result.getRecordMetadata();
                        if (!hasExistingSpan) {
                            span.setAttribute("kafka.partition", metadata.partition())
                                .setAttribute("kafka.offset", metadata.offset());
                        }
                        log.debug("Message sent to topic {} partition {} offset {}", 
                            topicName, metadata.partition(), metadata.offset());
                    }
                    // Only end the span if we created it
                    if (!hasExistingSpan) {
                        span.end();
                    }
                });
        } catch (KafkaException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (!hasExistingSpan) {
                span.end();
            }
            log.error("Error on kafka producer with key: {}, message: {} and exception: {}", 
                key, message, e.getMessage());
            throw new KafkaProducerException("Error on kafka producer with key: " + key + " and message: " + message);
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    @PreDestroy
    public void close() {
        if (kafkaTemplate != null) {
            log.info("Closing kafka producer!");
            kafkaTemplate.destroy();
        }
    }
}
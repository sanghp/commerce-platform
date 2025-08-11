package com.commerce.platform.kafka.producer.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class TracingKafkaProducer<K extends Serializable, V extends SpecificRecordBase> {
    
    private final KafkaTemplate<K, V> kafkaTemplate;
    private final OpenTelemetry openTelemetry;
    
    // Sampling rate: trace all messages for end-to-end tracing
    private static final double SAMPLING_RATE = 1.0;
    
    private static final TextMapSetter<Headers> SETTER = (headers, key, value) -> {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    };
    
    public CompletableFuture<SendResult<K, V>> send(String topicName, K key, V message) {
        // Just propagate the current context - let Java Agent handle the span creation
        ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topicName, key, message);
        
        // Inject current trace context into Kafka headers
        W3CTraceContextPropagator.getInstance().inject(
            Context.current(), 
            producerRecord.headers(), 
            SETTER
        );
        
        Span currentSpan = Span.current();
        log.debug("Sending message to topic {} with traceId={}, spanId={}", 
            topicName, currentSpan.getSpanContext().getTraceId(), 
            currentSpan.getSpanContext().getSpanId());
        
        return kafkaTemplate.send(producerRecord);
    }
    
    // New method to send with explicit trace context
    public CompletableFuture<SendResult<K, V>> sendWithTraceContext(String topicName, K key, V message, 
                                                                     String traceId, String spanId) {
        ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topicName, key, message);
        
        // Create context from provided trace/span IDs
        if (traceId != null && spanId != null) {
            SpanContext parentContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled(),
                TraceState.getDefault()
            );
            
            Context contextWithParent = Context.current().with(Span.wrap(parentContext));
            
            // Inject the explicit context into Kafka headers
            W3CTraceContextPropagator.getInstance().inject(
                contextWithParent, 
                producerRecord.headers(), 
                SETTER
            );
            
            log.debug("Sending message to topic {} with explicit traceId={}, spanId={}", 
                topicName, traceId, spanId);
        } else {
            // Fallback to current context
            W3CTraceContextPropagator.getInstance().inject(
                Context.current(), 
                producerRecord.headers(), 
                SETTER
            );
            
            log.debug("Sending message to topic {} with current context", topicName);
        }
        
        return kafkaTemplate.send(producerRecord);
    }
}
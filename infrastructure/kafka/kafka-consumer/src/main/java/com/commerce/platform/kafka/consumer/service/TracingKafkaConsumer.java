package com.commerce.platform.kafka.consumer.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class TracingKafkaConsumer {
    
    private final OpenTelemetry openTelemetry;
    
    private static final TextMapGetter<Headers> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return () -> headers.toArray().length > 0 ? 
                java.util.Arrays.stream(headers.toArray())
                    .map(Header::key)
                    .iterator() : 
                java.util.Collections.emptyIterator();
        }
        
        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            if (header != null && header.value() != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
            return null;
        }
    };
    
    public <T> void consumeWithTracing(
        ConsumerRecord<String, T> record,
        String consumerGroup,
        Consumer<T> messageProcessor
    ) {
        Tracer tracer = openTelemetry.getTracer("kafka-consumer", "1.0.0");
        
        // Extract trace context from Kafka headers
        Context extractedContext = W3CTraceContextPropagator.getInstance()
            .extract(Context.current(), record.headers(), GETTER);
        
        SpanBuilder spanBuilder = tracer.spanBuilder("kafka.consume " + record.topic())
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.destination", record.topic())
            .setAttribute("messaging.operation", "consume")
            .setAttribute("messaging.consumer.group", consumerGroup)
            .setAttribute("messaging.kafka.partition", record.partition())
            .setAttribute("messaging.kafka.offset", record.offset());
        
        // Link to parent span if exists
        if (extractedContext != Context.root()) {
            spanBuilder.setParent(extractedContext);
        }
        
        Span span = spanBuilder.startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            log.debug("Consuming message from topic {} partition {} offset {} with traceId={}, spanId={}", 
                record.topic(), record.partition(), record.offset(),
                span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
            
            messageProcessor.accept(record.value());
            
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Error processing message from topic {}: {}", record.topic(), e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
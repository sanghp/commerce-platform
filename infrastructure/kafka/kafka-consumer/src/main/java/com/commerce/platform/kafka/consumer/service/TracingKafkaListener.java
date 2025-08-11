package com.commerce.platform.kafka.consumer.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
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
import java.util.Iterator;

@Slf4j
@Component
@RequiredArgsConstructor
public class TracingKafkaListener {
    
    private final OpenTelemetry openTelemetry;
    
    private static final TextMapGetter<Headers> GETTER = new TextMapGetter<Headers>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return () -> new Iterator<String>() {
                final Iterator<Header> iterator = headers.iterator();
                
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }
                
                @Override
                public String next() {
                    return iterator.next().key();
                }
            };
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
    
    public Span startConsumerSpan(ConsumerRecord<?, ?> record) {
        Tracer tracer = openTelemetry.getTracer("kafka-consumer", "1.0.0");
        
        Context extractedContext = W3CTraceContextPropagator.getInstance()
            .extract(Context.current(), record.headers(), GETTER);
        
        Span span = tracer.spanBuilder("kafka.consume " + record.topic())
            .setParent(extractedContext)
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "kafka")
            .setAttribute("messaging.source", record.topic())
            .setAttribute("messaging.operation", "receive")
            .setAttribute("kafka.partition", record.partition())
            .setAttribute("kafka.offset", record.offset())
            .setAttribute("messaging.kafka.message.key", record.key() != null ? record.key().toString() : "null")
            .startSpan();
        
        log.debug("Started consumer span for topic {} with traceId={}, spanId={}", 
            record.topic(), span.getSpanContext().getTraceId(), span.getSpanContext().getSpanId());
        
        return span;
    }
    
    public void handleMessage(ConsumerRecord<?, ?> record, MessageHandler handler) {
        Span span = startConsumerSpan(record);
        
        try (Scope scope = span.makeCurrent()) {
            try {
                handler.handle(record);
            } catch (Exception handlerException) {
                throw new RuntimeException("Error in message handler", handlerException);
            }
            log.debug("Message processed successfully from topic {} partition {} offset {}", 
                record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Error processing message from topic {}: {}", record.topic(), e.getMessage(), e);
            throw e;
        } finally {
            span.end();
        }
    }
    
    @FunctionalInterface
    public interface MessageHandler {
        void handle(ConsumerRecord<?, ?> record) throws Exception;
    }
}
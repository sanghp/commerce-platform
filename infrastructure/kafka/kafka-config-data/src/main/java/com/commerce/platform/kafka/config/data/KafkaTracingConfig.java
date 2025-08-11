package com.commerce.platform.kafka.config.data;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTracingConfig {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }
    
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("kafka-tracing", "1.0.0");
    }
    
    @Bean 
    public TextMapPropagator textMapPropagator() {
        return W3CTraceContextPropagator.getInstance();
    }
    
    @Bean
    public ContextPropagators contextPropagators(TextMapPropagator textMapPropagator) {
        return ContextPropagators.create(textMapPropagator);
    }
}
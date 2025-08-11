package com.commerce.platform.product.service.domain.outbox.scheduler;

import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.message.publisher.ProductReservationResponseMessagePublisher;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductOutboxScheduler implements OutboxScheduler {

    private final ProductOutboxHelper outboxHelper;
    private final ProductReservationResponseMessagePublisher responseMessagePublisher;
    
    @Value("${product-service.outbox-scheduler-batch-size:10}")
    private int batchSize;
    
    @Value("${product-service.outbox-processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;

    public ProductOutboxScheduler(ProductOutboxHelper outboxHelper,
                                             ProductReservationResponseMessagePublisher responseMessagePublisher) {
        this.outboxHelper = outboxHelper;
        this.responseMessagePublisher = responseMessagePublisher;
    }

    @Override
    @Scheduled(fixedRateString = "${product-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${product-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        // Don't use @Async - it breaks trace context
        outboxHelper.resetTimedOutMessages(processingTimeoutMinutes, batchSize);
        List<ProductOutboxMessage> messagesToProcess = outboxHelper.updateMessagesToProcessing(batchSize);
        
        if (!messagesToProcess.isEmpty()) {
            log.info("Processing {} ProductOutboxMessages with ids: {}",
                    messagesToProcess.size(),
                    messagesToProcess.stream()
                            .map(msg -> msg.getId().toString())
                            .collect(Collectors.joining(",")));
            
            messagesToProcess.forEach(this::publishMessage);
            
            log.info("{} ProductOutboxMessages processed", messagesToProcess.size());
        }
    }
    
    private void publishMessage(ProductOutboxMessage outboxMessage) {
        // Restore trace context if available
        Span span = null;
        Scope scope = null;
        
        if (outboxMessage.getTraceId() != null && outboxMessage.getSpanId() != null) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("product-service");
            SpanContext parentContext = SpanContext.createFromRemoteParent(
                    outboxMessage.getTraceId(),
                    outboxMessage.getSpanId(),
                    TraceFlags.getSampled(),
                    TraceState.getDefault()
            );
            Context context = Context.current().with(Span.wrap(parentContext));
            span = tracer.spanBuilder("outbox-publish")
                    .setParent(context)
                    .setAttribute("outbox.message.id", outboxMessage.getId().toString())
                    .setAttribute("outbox.message.type", outboxMessage.getType().toString())
                    .startSpan();
            scope = span.makeCurrent();
            
            log.info("Restored trace context for outbox message {}: traceId={}, spanId={}",
                    outboxMessage.getId(), outboxMessage.getTraceId(), outboxMessage.getSpanId());
        }
        
        try {
            responseMessagePublisher.publish(outboxMessage);
        } finally {
            if (span != null) {
                span.end();
            }
            if (scope != null) {
                scope.close();
            }
        }
    }

} 
package com.commerce.platform.payment.service.domain.outbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxScheduler;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler implements OutboxScheduler {
    
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentResponseMessagePublisher paymentResponseMessagePublisher;
    
    @Value("${payment-service.outbox-scheduler-batch-size:10}")
    private int batchSize;
    
    @Value("${payment-service.outbox-processing-timeout-minutes:5}")
    private int processingTimeoutMinutes;
    
    @Override
    @Scheduled(fixedRateString = "${payment-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${payment-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        // Don't use @Async - it breaks trace context
        paymentOutboxHelper.resetTimedOutMessages(processingTimeoutMinutes, batchSize);
        List<PaymentOutboxMessage> messagesToProcess = paymentOutboxHelper.updateMessagesToProcessing(batchSize);
        
        if (!messagesToProcess.isEmpty()) {
            log.info("Processing {} PaymentOutboxMessages with ids: {}",
                    messagesToProcess.size(),
                    messagesToProcess.stream()
                            .map(msg -> msg.getId().toString())
                            .collect(Collectors.joining(",")));
            
            messagesToProcess.forEach(this::publishMessage);
            
            log.info("{} PaymentOutboxMessages processed", messagesToProcess.size());
        }
    }
    
    private void publishMessage(PaymentOutboxMessage outboxMessage) {
        // Restore trace context if available
        Span span = null;
        Scope scope = null;
        
        if (outboxMessage.getTraceId() != null && outboxMessage.getSpanId() != null) {
            Tracer tracer = GlobalOpenTelemetry.getTracer("payment-service");
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
            ServiceMessageType messageType = outboxMessage.getType();
            if (Objects.requireNonNull(messageType) == ServiceMessageType.PAYMENT_RESPONSE) {
                paymentResponseMessagePublisher.publish(outboxMessage);
            } else {
                log.warn("Unknown outbox message type: {}", messageType);
            }
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
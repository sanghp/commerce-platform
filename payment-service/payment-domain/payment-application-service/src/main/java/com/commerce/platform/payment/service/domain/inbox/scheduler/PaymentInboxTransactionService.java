package com.commerce.platform.payment.service.domain.inbox.scheduler;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.domain.valueobject.PaymentOrderStatus;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.domain.PaymentRequestHelper;
import com.commerce.platform.payment.service.domain.dto.PaymentRequest;
import com.commerce.platform.payment.service.domain.entity.Payment;
import com.commerce.platform.payment.service.domain.event.PaymentEvent;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.mapper.PaymentDataMapper;
import com.commerce.platform.payment.service.domain.outbox.scheduler.PaymentOutboxHelper;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PaymentInboxTransactionService {
    
    private final PaymentInboxRepository paymentInboxRepository;
    private final PaymentRequestHelper paymentRequestHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentDataMapper paymentDataMapper;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    
    public PaymentInboxTransactionService(PaymentInboxRepository paymentInboxRepository,
                                        PaymentRequestHelper paymentRequestHelper,
                                        PaymentOutboxHelper paymentOutboxHelper,
                                        PaymentDataMapper paymentDataMapper,
                                        ObjectMapper objectMapper) {
        this.paymentInboxRepository = paymentInboxRepository;
        this.paymentRequestHelper = paymentRequestHelper;
        this.paymentOutboxHelper = paymentOutboxHelper;
        this.paymentDataMapper = paymentDataMapper;
        this.objectMapper = objectMapper;
        this.tracer = GlobalOpenTelemetry.getTracer("payment-inbox", "1.0.0");
    }
    
    @Transactional
    public boolean processNextMessage() {
        Span span = tracer.spanBuilder("payment.inbox.process")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("inbox.operation", "process_message")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            List<PaymentInboxMessage> messages = paymentInboxRepository
                    .findByStatusOrderByReceivedAtWithSkipLock(InboxStatus.RECEIVED, 1);
            
            if (messages.isEmpty()) {
                span.setAttribute("inbox.empty", true);
                return false;
            }
            
            PaymentInboxMessage inboxMessage = messages.getFirst();
            span.setAttribute("inbox.message.id", inboxMessage.getId().toString());
            span.setAttribute("inbox.saga.id", inboxMessage.getSagaId().toString());
            span.setAttribute("inbox.message.type", inboxMessage.getType().toString());
            
            ZonedDateTime processedAt = ZonedDateTime.now();
            
            try {
                if (inboxMessage.getType() == ServiceMessageType.PAYMENT_REQUEST) {
                    processPaymentRequest(inboxMessage);
                }
                
                inboxMessage.setStatus(InboxStatus.PROCESSED);
                inboxMessage.setProcessedAt(processedAt);
                
                log.info("Successfully processed inbox message: {} for saga: {} with traceId: {}", 
                        inboxMessage.getId(), inboxMessage.getSagaId(), span.getSpanContext().getTraceId());
                
            } catch (Exception e) {
                span.setStatus(StatusCode.ERROR, e.getMessage());
                span.recordException(e);
                log.error("Failed to process inbox message: {}", inboxMessage.getId(), e);
                
                inboxMessage.setStatus(InboxStatus.FAILED);
                inboxMessage.setErrorMessage(e.getMessage());
                inboxMessage.setRetryCount(inboxMessage.getRetryCount() + 1);
            }
            
            paymentInboxRepository.save(inboxMessage);
            return true;
        } finally {
            span.end();
        }
    }
    
    @Transactional
    public void retryFailedMessages(int maxRetryCount, int batchSize) {
        Span span = tracer.spanBuilder("payment.inbox.retry")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("inbox.operation", "retry_failed")
            .setAttribute("inbox.max_retry", maxRetryCount)
            .setAttribute("inbox.batch_size", batchSize)
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            List<PaymentInboxMessage> failedMessages = paymentInboxRepository
                    .findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus.FAILED, maxRetryCount, batchSize);
            
            span.setAttribute("inbox.retry.count", failedMessages.size());
            
            if (!failedMessages.isEmpty()) {
                log.info("Retrying {} failed messages with traceId: {}", 
                    failedMessages.size(), span.getSpanContext().getTraceId());
                
                failedMessages.forEach(message -> message.setStatus(InboxStatus.RECEIVED));
                
                paymentInboxRepository.saveAll(failedMessages);
            }
        } finally {
            span.end();
        }
    }
    
    private void processPaymentRequest(PaymentInboxMessage inboxMessage) throws Exception {
        PaymentRequest request = objectMapper.readValue(
                inboxMessage.getPayload(), 
                PaymentRequest.class
        );
        
        PaymentEvent paymentEvent;
        if (request.getPaymentOrderStatus() == PaymentOrderStatus.PENDING) {
            paymentEvent = paymentRequestHelper.persistPayment(request);
        } else if (request.getPaymentOrderStatus() == PaymentOrderStatus.CANCELLED) {
            paymentEvent = paymentRequestHelper.persistCancelPayment(request);
        } else {
            throw new IllegalStateException("Invalid payment order status: " + request.getPaymentOrderStatus());
        }
        
        paymentOutboxHelper.savePaymentOutboxMessage(
                ServiceMessageType.PAYMENT_RESPONSE,
                paymentDataMapper.paymentEventToPaymentResponse(paymentEvent, inboxMessage.getSagaId()),
                OutboxStatus.STARTED,
                inboxMessage.getSagaId()
        );
        
        log.info("Payment processed for order id: {} with status: {}", 
                paymentEvent.getPayment().getOrderId().getValue(),
                paymentEvent.getPayment().getPaymentStatus());
    }
} 
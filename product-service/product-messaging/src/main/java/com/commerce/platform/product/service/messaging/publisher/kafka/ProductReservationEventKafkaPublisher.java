package com.commerce.platform.product.service.messaging.publisher.kafka;

import com.commerce.platform.kafka.order.avro.model.ProductReservationResponseAvroModel;
import com.commerce.platform.kafka.producer.KafkaMessageHelper;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.product.service.domain.config.ProductServiceConfigData;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.outbox.model.ProductReservationResponseEventPayload;
import com.commerce.platform.product.service.domain.ports.output.message.publisher.ProductReservationResponseMessagePublisher;
import com.commerce.platform.product.service.messaging.mapper.ProductMessagingDataMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.commerce.platform.kafka.producer.service.TracingKafkaProducer;
import com.commerce.platform.product.service.domain.outbox.helper.ProductOutboxHelper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class ProductReservationEventKafkaPublisher implements ProductReservationResponseMessagePublisher {

    private final ProductServiceConfigData productServiceConfigData;
    private final TracingKafkaProducer<UUID, ProductReservationResponseAvroModel> tracingKafkaProducer;
    private final ProductMessagingDataMapper productMessagingDataMapper;
    private final KafkaMessageHelper kafkaMessageHelper;
    private final ProductOutboxHelper productOutboxHelper;

    public ProductReservationEventKafkaPublisher(ProductServiceConfigData productServiceConfigData,
                                                 @Qualifier("productReservationResponseTracingKafkaProducer") TracingKafkaProducer<UUID, ProductReservationResponseAvroModel> productReservationResponseTracingKafkaProducer,
                                                 ProductMessagingDataMapper productMessagingDataMapper,
                                                 KafkaMessageHelper kafkaMessageHelper,
                                                 ProductOutboxHelper productOutboxHelper) {
        this.productServiceConfigData = productServiceConfigData;
        this.tracingKafkaProducer = productReservationResponseTracingKafkaProducer;
        this.productMessagingDataMapper = productMessagingDataMapper;
        this.kafkaMessageHelper = kafkaMessageHelper;
        this.productOutboxHelper = productOutboxHelper;
    }

    @Override
    public void publish(ProductOutboxMessage outboxMessage) {
        var responseEventPayload =
                kafkaMessageHelper.getOrderEventPayload(outboxMessage.getPayload(),
                        ProductReservationResponseEventPayload.class);

        var sagaId = outboxMessage.getSagaId();

        log.info("Received ProductReservationResponseEvent for order id: {} and saga id: {} with traceId: {}",
                responseEventPayload.getOrderId(),
                sagaId, outboxMessage.getTraceId());

        // Restore TraceContext if available
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
            span = tracer.spanBuilder("publish-product-reservation-response")
                    .setParent(context)
                    .startSpan();
            scope = span.makeCurrent();
        }

        try {
            var productReservationResponseAvroModel = productMessagingDataMapper
                    .productReservationResponseEventToResponseAvroModel(outboxMessage.getMessageId(), sagaId, responseEventPayload);

            // Add TraceContext to Kafka headers
            Headers headers = new RecordHeaders();
            if (outboxMessage.getTraceId() != null) {
                headers.add("traceId", outboxMessage.getTraceId().getBytes(StandardCharsets.UTF_8));
            }
            if (outboxMessage.getSpanId() != null) {
                headers.add("spanId", outboxMessage.getSpanId().getBytes(StandardCharsets.UTF_8));
            }

            tracingKafkaProducer.send(productServiceConfigData.getProductReservationResponseTopicName(),
                    sagaId,
                    productReservationResponseAvroModel)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka callback invoked for message id: {} with status: SUCCESS", outboxMessage.getId());
                            productOutboxHelper.updateOutboxMessageStatus(outboxMessage.getId(), OutboxStatus.COMPLETED);
                        } else {
                            log.error("Failed to send message: {}", ex.getMessage());
                            productOutboxHelper.updateOutboxMessageStatus(outboxMessage.getId(), OutboxStatus.FAILED);
                        }
                    });

            log.info("ProductReservationResponseEventPayload sent to Kafka for order id: {} and saga id: {}",
                    responseEventPayload.getOrderId(), sagaId);
        } catch (Exception e) {
            log.error("Error while sending ProductReservationResponseEventPayload" +
                            " to kafka with order id: {} and saga id: {}, error: {}",
                    responseEventPayload.getOrderId(), sagaId, e.getMessage());
            productOutboxHelper.updateOutboxMessageStatus(outboxMessage.getId(), OutboxStatus.FAILED);
            if (span != null) {
                span.recordException(e);
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
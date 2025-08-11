package com.commerce.platform.order.service.dataaccess.outbox.mapper;

import com.commerce.platform.order.service.dataaccess.outbox.entity.OrderOutboxEntity;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxDataAccessMapper {

    public OrderOutboxEntity orderOutboxMessageToOutboxEntity(OrderOutboxMessage orderOutboxMessage) {
        return OrderOutboxEntity.builder()
                .id(orderOutboxMessage.getId())
                .messageId(orderOutboxMessage.getMessageId())
                .sagaId(orderOutboxMessage.getSagaId())
                .createdAt(orderOutboxMessage.getCreatedAt())
                .fetchedAt(orderOutboxMessage.getFetchedAt())
                .type(orderOutboxMessage.getType())
                .payload(orderOutboxMessage.getPayload())
                .outboxStatus(orderOutboxMessage.getOutboxStatus())
                .processedAt(orderOutboxMessage.getProcessedAt())
                .version(orderOutboxMessage.getVersion())
                .traceId(orderOutboxMessage.getTraceId())
                .spanId(orderOutboxMessage.getSpanId())
                .build();
    }

    public OrderOutboxMessage orderOutboxEntityToOrderOutboxMessage(OrderOutboxEntity orderOutboxEntity) {
        return OrderOutboxMessage.builder()
                .id(orderOutboxEntity.getId())
                .messageId(orderOutboxEntity.getMessageId())
                .sagaId(orderOutboxEntity.getSagaId())
                .createdAt(orderOutboxEntity.getCreatedAt())
                .fetchedAt(orderOutboxEntity.getFetchedAt())
                .type(orderOutboxEntity.getType())
                .payload(orderOutboxEntity.getPayload())
                .outboxStatus(orderOutboxEntity.getOutboxStatus())
                .processedAt(orderOutboxEntity.getProcessedAt())
                .version(orderOutboxEntity.getVersion())
                .traceId(orderOutboxEntity.getTraceId())
                .spanId(orderOutboxEntity.getSpanId())
                .build();
    }
}
package com.commerce.platform.product.service.dataaccess.outbox.mapper;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.dataaccess.outbox.entity.ProductOutboxEntity;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class ProductOutboxDataAccessMapper {

    public ProductOutboxEntity productOutboxMessageToOutboxEntity(ProductOutboxMessage
                                                                                outboxMessage) {
        return ProductOutboxEntity.builder()
                .id(outboxMessage.getId())
                .sagaId(outboxMessage.getSagaId())
                .createdAt(outboxMessage.getCreatedAt())
                .processedAt(outboxMessage.getProcessedAt())
                .type(outboxMessage.getType())
                .payload(outboxMessage.getPayload())
                .outboxStatus(outboxMessage.getOutboxStatus())
                .version(outboxMessage.getVersion())
                .build();
    }

    public ProductOutboxMessage productOutboxEntityToOutboxMessage(ProductOutboxEntity
                                                                                outboxEntity) {
        return ProductOutboxMessage.builder()
                .id(outboxEntity.getId())
                .sagaId(outboxEntity.getSagaId())
                .createdAt(outboxEntity.getCreatedAt())
                .processedAt(outboxEntity.getProcessedAt())
                .type(outboxEntity.getType())
                .payload(outboxEntity.getPayload())
                .outboxStatus(outboxEntity.getOutboxStatus())
                .version(outboxEntity.getVersion())
                .build();
    }
} 
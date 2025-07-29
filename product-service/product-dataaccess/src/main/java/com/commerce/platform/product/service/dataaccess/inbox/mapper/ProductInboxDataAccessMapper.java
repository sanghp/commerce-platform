package com.commerce.platform.product.service.dataaccess.inbox.mapper;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.dataaccess.inbox.entity.ProductInboxEntity;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import org.springframework.stereotype.Component;

@Component
public class ProductInboxDataAccessMapper {

    public ProductInboxEntity productInboxMessageToInboxEntity(ProductInboxMessage inboxMessage) {
        return ProductInboxEntity.builder()
                .id(inboxMessage.getId())
                .sagaId(inboxMessage.getSagaId())
                .eventType(inboxMessage.getEventType())
                .payload(inboxMessage.getPayload())
                .processedAt(inboxMessage.getProcessedAt())
                .build();
    }

    public ProductInboxMessage productInboxEntityToInboxMessage(ProductInboxEntity inboxEntity) {
        return ProductInboxMessage.builder()
                .id(inboxEntity.getId())
                .sagaId(inboxEntity.getSagaId())
                .eventType(inboxEntity.getEventType())
                .payload(inboxEntity.getPayload())
                .processedAt(inboxEntity.getProcessedAt())
                .build();
    }
} 
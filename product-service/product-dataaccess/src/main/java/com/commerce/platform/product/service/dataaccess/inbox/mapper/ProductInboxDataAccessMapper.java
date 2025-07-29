package com.commerce.platform.product.service.dataaccess.inbox.mapper;

import com.commerce.platform.product.service.dataaccess.inbox.entity.ProductInboxEntity;
import com.commerce.platform.product.service.domain.inbox.model.ProductInboxMessage;
import org.springframework.stereotype.Component;

@Component
public class ProductInboxDataAccessMapper {
    
    public ProductInboxEntity productInboxMessageToProductInboxEntity(ProductInboxMessage productInboxMessage) {
        return ProductInboxEntity.builder()
                .id(productInboxMessage.getId())
                .sagaId(productInboxMessage.getSagaId())
                .eventType(productInboxMessage.getEventType())
                .payload(productInboxMessage.getPayload())
                .status(productInboxMessage.getStatus())
                .receivedAt(productInboxMessage.getReceivedAt())
                .processedAt(productInboxMessage.getProcessedAt())
                .retryCount(productInboxMessage.getRetryCount())
                .errorMessage(productInboxMessage.getErrorMessage())
                .version(productInboxMessage.getVersion())
                .build();
    }
    
    public ProductInboxMessage productInboxEntityToProductInboxMessage(ProductInboxEntity productInboxEntity) {
        return ProductInboxMessage.builder()
                .id(productInboxEntity.getId())
                .sagaId(productInboxEntity.getSagaId())
                .eventType(productInboxEntity.getEventType())
                .payload(productInboxEntity.getPayload())
                .status(productInboxEntity.getStatus())
                .receivedAt(productInboxEntity.getReceivedAt())
                .processedAt(productInboxEntity.getProcessedAt())
                .retryCount(productInboxEntity.getRetryCount())
                .errorMessage(productInboxEntity.getErrorMessage())
                .version(productInboxEntity.getVersion())
                .build();
    }
}
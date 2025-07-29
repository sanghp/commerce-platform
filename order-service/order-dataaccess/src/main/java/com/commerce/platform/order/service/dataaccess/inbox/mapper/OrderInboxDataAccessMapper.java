package com.commerce.platform.order.service.dataaccess.inbox.mapper;

import com.commerce.platform.order.service.dataaccess.inbox.entity.OrderInboxEntity;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderInboxDataAccessMapper {

    public OrderInboxEntity orderInboxMessageToOrderInboxEntity(OrderInboxMessage orderInboxMessage) {
        return OrderInboxEntity.builder()
                .id(orderInboxMessage.getId())
                .sagaId(orderInboxMessage.getSagaId())
                .eventType(orderInboxMessage.getEventType())
                .payload(orderInboxMessage.getPayload())
                .status(orderInboxMessage.getStatus())
                .receivedAt(orderInboxMessage.getReceivedAt())
                .processedAt(orderInboxMessage.getProcessedAt())
                .retryCount(orderInboxMessage.getRetryCount())
                .errorMessage(orderInboxMessage.getErrorMessage())
                .version(orderInboxMessage.getVersion())
                .build();
    }

    public OrderInboxMessage orderInboxEntityToOrderInboxMessage(OrderInboxEntity orderInboxEntity) {
        return OrderInboxMessage.builder()
                .id(orderInboxEntity.getId())
                .sagaId(orderInboxEntity.getSagaId())
                .eventType(orderInboxEntity.getEventType())
                .payload(orderInboxEntity.getPayload())
                .status(orderInboxEntity.getStatus())
                .receivedAt(orderInboxEntity.getReceivedAt())
                .processedAt(orderInboxEntity.getProcessedAt())
                .retryCount(orderInboxEntity.getRetryCount())
                .errorMessage(orderInboxEntity.getErrorMessage())
                .version(orderInboxEntity.getVersion())
                .build();
    }
}
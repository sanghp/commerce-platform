package com.commerce.platform.order.service.dataaccess.inbox.mapper;

import com.commerce.platform.order.service.dataaccess.inbox.entity.OrderInboxEntity;
import com.commerce.platform.order.service.domain.inbox.model.OrderInboxMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderInboxDataAccessMapper {

    public OrderInboxEntity orderInboxMessageToOrderInboxEntity(OrderInboxMessage orderInboxMessage) {
        return OrderInboxEntity.builder()
                .id(orderInboxMessage.getId())
                .messageId(orderInboxMessage.getMessageId())
                .sagaId(orderInboxMessage.getSagaId())
                .type(orderInboxMessage.getType())
                .payload(orderInboxMessage.getPayload())
                .status(orderInboxMessage.getStatus())
                .receivedAt(orderInboxMessage.getReceivedAt())
                .processedAt(orderInboxMessage.getProcessedAt())
                .retryCount(orderInboxMessage.getRetryCount())
                .errorMessage(orderInboxMessage.getErrorMessage())
                .build();
    }

    public OrderInboxMessage orderInboxEntityToOrderInboxMessage(OrderInboxEntity orderInboxEntity) {
        return OrderInboxMessage.builder()
                .id(orderInboxEntity.getId())
                .messageId(orderInboxEntity.getMessageId())
                .sagaId(orderInboxEntity.getSagaId())
                .type(orderInboxEntity.getType())
                .payload(orderInboxEntity.getPayload())
                .status(orderInboxEntity.getStatus())
                .receivedAt(orderInboxEntity.getReceivedAt())
                .processedAt(orderInboxEntity.getProcessedAt())
                .retryCount(orderInboxEntity.getRetryCount())
                .errorMessage(orderInboxEntity.getErrorMessage())
                .build();
    }
}
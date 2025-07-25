package com.commerce.platform.order.service.domain.ports.output.repository;

import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReservationOutboxRepository {
    ProductReservationOutboxMessage save(ProductReservationOutboxMessage productReservationOutboxMessage);

    Optional<List<ProductReservationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String type,
                                                                                      OutboxStatus outboxStatus,
                                                                                      SagaStatus... sagaStatus);
    Optional<ProductReservationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                          UUID sagaId,
                                                                          SagaStatus... sagaStatus);
    void deleteByTypeAndOutboxStatusAndSagaStatus(String type,
                                                  OutboxStatus outboxStatus,
                                                  SagaStatus... sagaStatus);
    

}

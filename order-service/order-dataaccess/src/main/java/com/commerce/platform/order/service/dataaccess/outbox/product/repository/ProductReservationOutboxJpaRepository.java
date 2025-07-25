package com.commerce.platform.order.service.dataaccess.outbox.product.repository;

import com.commerce.platform.order.service.dataaccess.outbox.product.entity.ProductReservationOutboxEntity;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReservationOutboxJpaRepository extends JpaRepository<ProductReservationOutboxEntity, UUID> {

    Optional<List<ProductReservationOutboxEntity>> findByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                                                  OutboxStatus outboxStatus,
                                                                                  List<SagaStatus> sagaStatus);

    Optional<ProductReservationOutboxEntity> findByTypeAndSagaIdAndSagaStatusIn(String type,
                                                                                UUID sagaId,
                                                                                List<SagaStatus> sagaStatus);

    void deleteByTypeAndOutboxStatusAndSagaStatusIn(String type,
                                                    OutboxStatus outboxStatus,
                                                    List<SagaStatus> sagaStatus);
    


}

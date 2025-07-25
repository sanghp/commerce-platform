package com.commerce.platform.order.service.dataaccess.outbox.product.adapter;

import com.commerce.platform.order.service.dataaccess.outbox.product.exception.ProductReservationOutboxNotFoundException;
import com.commerce.platform.order.service.dataaccess.outbox.product.mapper.ProductReservationOutboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.outbox.product.repository.ProductReservationOutboxJpaRepository;
import com.commerce.platform.order.service.domain.outbox.model.product.ProductReservationOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.ProductReservationOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.saga.SagaStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductReservationOutboxRepositoryImpl implements ProductReservationOutboxRepository {

    private final ProductReservationOutboxJpaRepository reservationOutboxJpaRepository;
    private final ProductReservationOutboxDataAccessMapper reservationOutboxDataAccessMapper;

    public ProductReservationOutboxRepositoryImpl(ProductReservationOutboxJpaRepository reservationOutboxJpaRepository,
                                                  ProductReservationOutboxDataAccessMapper reservationOutboxDataAccessMapper) {
        this.reservationOutboxJpaRepository = reservationOutboxJpaRepository;
        this.reservationOutboxDataAccessMapper = reservationOutboxDataAccessMapper;
    }

    @Override
    public ProductReservationOutboxMessage save(ProductReservationOutboxMessage reservationOutboxMessage) {
        return reservationOutboxDataAccessMapper
                .productReservationEntityToOutboxMessage(reservationOutboxJpaRepository
                        .save(reservationOutboxDataAccessMapper
                                .orderCreatedOutboxMessageToOutboxEntity(reservationOutboxMessage)));
    }

    @Override
    public Optional<List<ProductReservationOutboxMessage>> findByTypeAndOutboxStatusAndSagaStatus(String sagaType,
                                                                                       OutboxStatus outboxStatus,
                                                                       SagaStatus... sagaStatus) {
        return Optional.of(reservationOutboxJpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn(sagaType, outboxStatus,
                Arrays.asList(sagaStatus))
                .orElseThrow(() -> new ProductReservationOutboxNotFoundException("Approval outbox object " +
                        "could be found for saga type " + sagaType))
                .stream()
                .map(reservationOutboxDataAccessMapper::productReservationEntityToOutboxMessage)
                .collect(Collectors.toList()));
    }

    @Override
    public Optional<ProductReservationOutboxMessage> findByTypeAndSagaIdAndSagaStatus(String type,
                                                                                 UUID sagaId,
                                                                                 SagaStatus... sagaStatus) {
        return reservationOutboxJpaRepository
                .findByTypeAndSagaIdAndSagaStatusIn(type, sagaId,
                        Arrays.asList(sagaStatus))
                .map(reservationOutboxDataAccessMapper::productReservationEntityToOutboxMessage);

    }

    @Override
    public void deleteByTypeAndOutboxStatusAndSagaStatus(String type, OutboxStatus outboxStatus, SagaStatus... sagaStatus) {
        reservationOutboxJpaRepository.deleteByTypeAndOutboxStatusAndSagaStatusIn(type, outboxStatus,
                Arrays.asList(sagaStatus));
    }


}

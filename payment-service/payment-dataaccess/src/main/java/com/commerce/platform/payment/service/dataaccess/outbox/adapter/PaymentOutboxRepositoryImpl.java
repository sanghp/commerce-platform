package com.commerce.platform.payment.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.mapper.PaymentOutboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.outbox.repository.PaymentOutboxJpaRepository;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class PaymentOutboxRepositoryImpl implements PaymentOutboxRepository {
    
    private final PaymentOutboxJpaRepository paymentOutboxJpaRepository;
    private final PaymentOutboxDataAccessMapper paymentOutboxDataAccessMapper;
    
    @Override
    public PaymentOutboxMessage save(PaymentOutboxMessage paymentOutboxMessage) {
        return paymentOutboxDataAccessMapper.outboxEntityToPaymentOutboxMessage(
                paymentOutboxJpaRepository.save(
                        paymentOutboxDataAccessMapper.paymentOutboxMessageToOutboxEntity(paymentOutboxMessage)));
    }
    
    
    @Override
    public List<PaymentOutboxMessage> findByTypeAndOutboxStatus(ServiceMessageType type, OutboxStatus outboxStatus) {
        return paymentOutboxJpaRepository.findByTypeAndOutboxStatusOrderByCreatedAt(type, outboxStatus)
                .stream()
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentOutboxMessage> saveAll(List<PaymentOutboxMessage> paymentOutboxMessages) {
        return paymentOutboxJpaRepository.saveAll(
                paymentOutboxMessages.stream()
                        .map(paymentOutboxDataAccessMapper::paymentOutboxMessageToOutboxEntity)
                        .collect(Collectors.toList())
        ).stream()
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<PaymentOutboxMessage> findById(UUID id) {
        return paymentOutboxJpaRepository.findById(id)
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage);
    }
    
    @Override
    public List<PaymentOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return paymentOutboxJpaRepository.findByOutboxStatusOrderByCreatedAt(outboxStatus)
                .stream()
                .limit(limit)
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        return paymentOutboxJpaRepository.findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(outboxStatus, fetchedAtBefore)
                .stream()
                .limit(limit)
                .map(paymentOutboxDataAccessMapper::outboxEntityToPaymentOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        var entitiesToDelete = paymentOutboxJpaRepository
                .findByOutboxStatusOrderByCreatedAt(outboxStatus)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        
        paymentOutboxJpaRepository.deleteAll(entitiesToDelete);
        return entitiesToDelete.size();
    }
}
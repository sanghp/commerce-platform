package com.commerce.platform.payment.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.dataaccess.inbox.mapper.PaymentInboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.inbox.repository.PaymentInboxJpaRepository;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class PaymentInboxRepositoryImpl implements PaymentInboxRepository {
    
    private final PaymentInboxJpaRepository paymentInboxJpaRepository;
    private final PaymentInboxDataAccessMapper paymentInboxDataAccessMapper;
    
    @Override
    public PaymentInboxMessage save(PaymentInboxMessage paymentInboxMessage) {
        return paymentInboxDataAccessMapper.inboxEntityToPaymentInboxMessage(
                paymentInboxJpaRepository.save(
                        paymentInboxDataAccessMapper.paymentInboxMessageToInboxEntity(paymentInboxMessage)));
    }
    
    @Override
    public Optional<PaymentInboxMessage> findByMessageId(UUID messageId) {
        return paymentInboxJpaRepository.findByMessageId(messageId)
                .map(paymentInboxDataAccessMapper::inboxEntityToPaymentInboxMessage);
    }
    
    @Override
    public List<PaymentInboxMessage> saveAll(List<PaymentInboxMessage> paymentInboxMessages) {
        return paymentInboxJpaRepository.saveAll(
                paymentInboxMessages.stream()
                        .map(paymentInboxDataAccessMapper::paymentInboxMessageToInboxEntity)
                        .collect(Collectors.toList())
        ).stream()
                .map(paymentInboxDataAccessMapper::inboxEntityToPaymentInboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusOrderByReceivedAtWithSkipLock(InboxStatus status, int limit) {
        return paymentInboxJpaRepository.findByStatusOrderByReceivedAtWithSkipLock(status.name(), PageRequest.of(0, limit))
                .stream()
                .map(paymentInboxDataAccessMapper::inboxEntityToPaymentInboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        return paymentInboxJpaRepository.findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(status.name(), maxRetryCount, PageRequest.of(0, limit))
                .stream()
                .map(paymentInboxDataAccessMapper::inboxEntityToPaymentInboxMessage)
                .collect(Collectors.toList());
    }
}
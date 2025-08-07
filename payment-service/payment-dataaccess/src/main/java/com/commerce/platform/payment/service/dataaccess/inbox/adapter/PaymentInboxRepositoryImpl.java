package com.commerce.platform.payment.service.dataaccess.inbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;
import com.commerce.platform.inbox.InboxStatus;
import com.commerce.platform.payment.service.dataaccess.inbox.mapper.PaymentInboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.inbox.repository.PaymentInboxJpaRepository;
import com.commerce.platform.payment.service.domain.inbox.model.PaymentInboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentInboxRepositoryImpl implements PaymentInboxRepository {
    
    private final PaymentInboxJpaRepository paymentInboxJpaRepository;
    private final PaymentInboxDataAccessMapper paymentInboxDataAccessMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
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
        if (paymentInboxMessages.isEmpty()) {
            return paymentInboxMessages;
        }
        
        String sql = "INSERT IGNORE INTO payment_inbox (id, message_id, saga_id, type, payload, status, received_at, retry_count) " +
                     "VALUES (UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), UNHEX(REPLACE(?, '-', '')), ?, ?, ?, ?, ?)";
        
        int insertedCount = 0;
        for (PaymentInboxMessage message : paymentInboxMessages) {
            int result = jdbcTemplate.update(sql,
                message.getId().toString(),
                message.getMessageId().toString(),
                message.getSagaId().toString(),
                message.getType().name(),
                message.getPayload(),
                message.getStatus().name(),
                Timestamp.from(message.getReceivedAt().toInstant()),
                message.getRetryCount()
            );
            insertedCount += result;
        }
        
        log.info("Inserted {} new messages out of {} total messages to inbox", insertedCount, paymentInboxMessages.size());
        
        return paymentInboxMessages;
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
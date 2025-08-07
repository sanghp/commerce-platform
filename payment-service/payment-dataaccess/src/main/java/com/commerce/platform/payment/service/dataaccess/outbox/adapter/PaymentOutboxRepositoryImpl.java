package com.commerce.platform.payment.service.dataaccess.outbox.adapter;

import com.commerce.platform.outbox.OutboxStatus;
import com.commerce.platform.payment.service.dataaccess.outbox.mapper.PaymentOutboxDataAccessMapper;
import com.commerce.platform.payment.service.dataaccess.outbox.repository.PaymentOutboxJpaRepository;
import com.commerce.platform.payment.service.domain.outbox.model.PaymentOutboxMessage;
import com.commerce.platform.payment.service.domain.ports.output.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public PaymentOutboxMessage save(PaymentOutboxMessage paymentOutboxMessage) {
        return paymentOutboxDataAccessMapper.outboxEntityToPaymentOutboxMessage(
                paymentOutboxJpaRepository.save(
                        paymentOutboxDataAccessMapper.paymentOutboxMessageToOutboxEntity(paymentOutboxMessage)));
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
    public List<PaymentOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        return paymentOutboxJpaRepository.findByOutboxStatusWithSkipLock(outboxStatus.name(), limit)
                .stream()
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
    
    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }
        
        String sql = "UPDATE payment_outbox SET outbox_status = ?, fetched_at = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
        
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, status.name());
                if (fetchedAt != null) {
                    ps.setTimestamp(2, java.sql.Timestamp.from(fetchedAt.toInstant()));
                } else {
                    ps.setNull(2, java.sql.Types.TIMESTAMP);
                }
                ps.setString(3, ids.get(i).toString());
            }
            
            @Override
            public int getBatchSize() {
                return ids.size();
            }
        });
        
        return java.util.Arrays.stream(updateCounts).sum();
    }
}
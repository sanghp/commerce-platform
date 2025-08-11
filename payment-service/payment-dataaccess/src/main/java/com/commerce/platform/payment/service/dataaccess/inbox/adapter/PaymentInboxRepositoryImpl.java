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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, status, received_at, processed_at, retry_count, error_message " +
                    "FROM payment_inbox WHERE status = ? ORDER BY received_at LIMIT ? FOR UPDATE SKIP LOCKED";
        
        return jdbcTemplate.query(sql, new Object[]{status.name(), limit}, new PaymentInboxRowMapper());
    }
    
    @Override
    public List<PaymentInboxMessage> findByStatusAndRetryCountLessThanOrderByReceivedAtWithSkipLock(InboxStatus status, int maxRetryCount, int limit) {
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, status, received_at, processed_at, retry_count, error_message " +
                    "FROM payment_inbox WHERE status = ? AND retry_count < ? ORDER BY received_at LIMIT ? FOR UPDATE SKIP LOCKED";
        
        return jdbcTemplate.query(sql, new Object[]{status.name(), maxRetryCount, limit}, new PaymentInboxRowMapper());
    }
    
    private static class PaymentInboxRowMapper implements RowMapper<PaymentInboxMessage> {
        @Override
        public PaymentInboxMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PaymentInboxMessage.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .messageId(UUID.fromString(rs.getString("message_id")))
                    .sagaId(UUID.fromString(rs.getString("saga_id")))
                    .type(ServiceMessageType.valueOf(rs.getString("type")))
                    .payload(rs.getString("payload"))
                    .status(InboxStatus.valueOf(rs.getString("status")))
                    .receivedAt(ZonedDateTime.ofInstant(rs.getTimestamp("received_at").toInstant(), ZoneId.of("UTC")))
                    .processedAt(rs.getTimestamp("processed_at") != null ? 
                            ZonedDateTime.ofInstant(rs.getTimestamp("processed_at").toInstant(), ZoneId.of("UTC")) : null)
                    .retryCount(rs.getInt("retry_count"))
                    .errorMessage(rs.getString("error_message"))
                    .build();
        }
    }
}
package com.commerce.platform.product.service.dataaccess.outbox.adapter;

import com.commerce.platform.domain.event.ServiceMessageType;

import com.commerce.platform.product.service.dataaccess.outbox.entity.ProductOutboxEntity;
import com.commerce.platform.product.service.dataaccess.outbox.mapper.ProductOutboxDataAccessMapper;
import com.commerce.platform.product.service.dataaccess.outbox.repository.ProductOutboxJpaRepository;
import com.commerce.platform.product.service.domain.outbox.model.ProductOutboxMessage;
import com.commerce.platform.product.service.domain.ports.output.repository.ProductOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductOutboxRepositoryImpl implements ProductOutboxRepository {

    private final ProductOutboxJpaRepository outboxJpaRepository;
    private final ProductOutboxDataAccessMapper outboxDataAccessMapper;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ProductOutboxMessage save(ProductOutboxMessage outboxMessage) {
        return outboxDataAccessMapper
                .productOutboxEntityToOutboxMessage(outboxJpaRepository
                        .save(outboxDataAccessMapper
                                .productOutboxMessageToOutboxEntity(outboxMessage)));
    }
    
    @Override
    public List<ProductOutboxMessage> saveAll(List<ProductOutboxMessage> outboxMessages) {
        return outboxJpaRepository
                .saveAll(outboxMessages.stream()
                        .map(outboxDataAccessMapper::productOutboxMessageToOutboxEntity)
                        .collect(Collectors.toList()))
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByOutboxStatus(OutboxStatus outboxStatus) {
        outboxJpaRepository.deleteByOutboxStatus(outboxStatus);
    }

    @Override
    public List<ProductOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return outboxJpaRepository.findByOutboxStatus(outboxStatus, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ProductOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, outbox_status, created_at, processed_at, fetched_at, version " +
                    "FROM product_outbox WHERE outbox_status = ? ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED";
        
        return jdbcTemplate.query(sql, new Object[]{outboxStatus.name(), limit}, new ProductOutboxRowMapper());
    }
    
    @Override
    public List<ProductOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus, ZonedDateTime fetchedAtBefore, int limit) {
        String sql = "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(message_id) as message_id, BIN_TO_UUID(saga_id) as saga_id, " +
                    "type, payload, outbox_status, created_at, processed_at, fetched_at, version " +
                    "FROM product_outbox WHERE outbox_status = ? AND fetched_at < ? ORDER BY created_at LIMIT ?";
        
        return jdbcTemplate.query(sql, new Object[]{outboxStatus.name(), 
                java.sql.Timestamp.from(fetchedAtBefore.toInstant()), limit}, new ProductOutboxRowMapper());
    }
    
    @Override
    public Optional<ProductOutboxMessage> findById(UUID id) {
        return outboxJpaRepository.findById(id)
                .map(outboxDataAccessMapper::productOutboxEntityToOutboxMessage);
    }
    
    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }
        
        String sql = "UPDATE product_outbox SET outbox_status = ?, fetched_at = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";
        
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
    
    private static class ProductOutboxRowMapper implements RowMapper<ProductOutboxMessage> {
        @Override
        public ProductOutboxMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ProductOutboxMessage.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .messageId(UUID.fromString(rs.getString("message_id")))
                    .sagaId(UUID.fromString(rs.getString("saga_id")))
                    .type(ServiceMessageType.valueOf(rs.getString("type")))
                    .payload(rs.getString("payload"))
                    .outboxStatus(OutboxStatus.valueOf(rs.getString("outbox_status")))
                    .createdAt(ZonedDateTime.ofInstant(rs.getTimestamp("created_at").toInstant(), ZoneId.of("UTC")))
                    .processedAt(rs.getTimestamp("processed_at") != null ? 
                            ZonedDateTime.ofInstant(rs.getTimestamp("processed_at").toInstant(), ZoneId.of("UTC")) : null)
                    .fetchedAt(rs.getTimestamp("fetched_at") != null ? 
                            ZonedDateTime.ofInstant(rs.getTimestamp("fetched_at").toInstant(), ZoneId.of("UTC")) : null)
                    .version(rs.getInt("version"))
                    .build();
        }
    }
} 
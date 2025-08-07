package com.commerce.platform.order.service.dataaccess.outbox.adapter;

import com.commerce.platform.order.service.dataaccess.outbox.entity.OrderOutboxEntity;
import com.commerce.platform.order.service.dataaccess.outbox.mapper.OrderOutboxDataAccessMapper;
import com.commerce.platform.order.service.dataaccess.outbox.repository.OrderOutboxJpaRepository;
import com.commerce.platform.order.service.domain.outbox.model.OrderOutboxMessage;
import com.commerce.platform.order.service.domain.ports.output.repository.OrderOutboxRepository;
import com.commerce.platform.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private final OrderOutboxJpaRepository orderOutboxJpaRepository;
    private final OrderOutboxDataAccessMapper orderOutboxDataAccessMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public OrderOutboxMessage save(OrderOutboxMessage orderOutboxMessage) {
        return orderOutboxDataAccessMapper
                .orderOutboxEntityToOrderOutboxMessage(orderOutboxJpaRepository
                        .save(orderOutboxDataAccessMapper
                                .orderOutboxMessageToOutboxEntity(orderOutboxMessage)));
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        return orderOutboxJpaRepository.findByOutboxStatusOrderByCreatedAt(outboxStatus, PageRequest.of(0, limit))
                .stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatusWithSkipLock(OutboxStatus outboxStatus, int limit) {
        return orderOutboxJpaRepository.findByOutboxStatusWithSkipLock(outboxStatus.name(), limit)
                .stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderOutboxMessage> saveAll(List<OrderOutboxMessage> orderOutboxMessages) {
        List<OrderOutboxEntity> entities = orderOutboxMessages.stream()
                .map(orderOutboxDataAccessMapper::orderOutboxMessageToOutboxEntity)
                .collect(Collectors.toList());
        return orderOutboxJpaRepository.saveAll(entities).stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderOutboxMessage> findByOutboxStatusAndFetchedAtBefore(OutboxStatus outboxStatus,
                                                                         ZonedDateTime fetchedAtBefore,
                                                                         int limit) {
        return orderOutboxJpaRepository
                .findByOutboxStatusAndFetchedAtBeforeOrderByCreatedAt(outboxStatus, fetchedAtBefore, PageRequest.of(0, limit))
                .stream()
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteByOutboxStatus(OutboxStatus outboxStatus, int limit) {
        List<OrderOutboxEntity> entitiesToDelete = orderOutboxJpaRepository
                .findByOutboxStatusOrderByCreatedAt(outboxStatus, PageRequest.of(0, limit));
        int deletedCount = entitiesToDelete.size();
        if (deletedCount > 0) {
            orderOutboxJpaRepository.deleteAllInBatch(entitiesToDelete);
        }
        return deletedCount;
    }

    @Override
    public Optional<OrderOutboxMessage> findById(UUID id) {
        return orderOutboxJpaRepository.findById(id)
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOrderOutboxMessage);
    }

    @Override
    @Transactional
    public int bulkUpdateStatusAndFetchedAt(List<UUID> ids, OutboxStatus status, ZonedDateTime fetchedAt) {
        if (ids.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE order_outbox SET outbox_status = ?, fetched_at = ? WHERE id = UNHEX(REPLACE(?, '-', ''))";

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
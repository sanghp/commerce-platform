SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `payments`;
DROP TABLE IF EXISTS `credits`;
DROP TABLE IF EXISTS `payment_outbox`;
DROP TABLE IF EXISTS `payment_inbox`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `payments`
(
    id               BINARY(16)     NOT NULL,
    customer_id      BINARY(16)     NOT NULL,
    order_id         BINARY(16)     NOT NULL,
    price            DECIMAL(10,2)  NOT NULL,
    payment_status   ENUM ('COMPLETED', 'CANCELLED', 'FAILED') NOT NULL,
    created_at       TIMESTAMP(6)   NOT NULL,
    failure_messages TEXT,

    PRIMARY KEY (id),
    CONSTRAINT `uk_payments_order_id` UNIQUE (order_id)
);

CREATE TABLE `credits`
(
    id               BINARY(16)     NOT NULL,
    customer_id      BINARY(16)     NOT NULL,
    amount           DECIMAL(10,2)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT `uk_credits_customer_id` UNIQUE (customer_id)
);

CREATE TABLE `payment_outbox`
(
    id            BINARY(16)   NOT NULL,
    message_id    BINARY(16)   NOT NULL,
    saga_id       BINARY(16)   NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    fetched_at    TIMESTAMP(6),
    processed_at  TIMESTAMP(6),
    type          VARCHAR(255) NOT NULL,
    payload       JSON         NOT NULL,
    outbox_status ENUM ('STARTED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL,
    version       INT          NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT `uk_payment_outbox_message_id` UNIQUE (message_id),
    INDEX `idx_payment_outbox_saga_id` (saga_id),
    INDEX `idx_payment_outbox_fetch` (outbox_status, fetched_at)
);

CREATE TABLE `payment_inbox`
(
    id            BINARY(16)   NOT NULL,
    message_id    BINARY(16)   NOT NULL,
    saga_id       BINARY(16)   NOT NULL,
    type          VARCHAR(255) NOT NULL,
    payload       JSON         NOT NULL,
    status        ENUM('RECEIVED', 'PROCESSED', 'FAILED') NOT NULL DEFAULT 'RECEIVED',
    received_at   TIMESTAMP(6) NOT NULL,
    processed_at  TIMESTAMP(6),
    retry_count   INT DEFAULT 0,
    error_message TEXT,

    PRIMARY KEY (id),
    CONSTRAINT `uk_payment_inbox_message_id` UNIQUE (message_id),
    INDEX `idx_payment_inbox_saga_id` (saga_id),
    INDEX `idx_payment_inbox_status_received` (status, received_at),
    INDEX `idx_payment_inbox_status_retry_received` (status, retry_count, received_at)
);

-- 초기 테스트 데이터 삽입
INSERT INTO `credits` (id, customer_id, amount) VALUES
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', '')), 
     UNHEX(REPLACE('3fa85f64-5717-4562-b3fc-2c963f66afa6', '-', '')), 
     1000000.00),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')), 
     UNHEX(REPLACE('123e4567-e89b-12d3-a456-426614174000', '-', '')), 
     500000.00),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440003', '-', '')), 
     UNHEX(REPLACE('987fcdeb-51a2-43d1-b4c5-316227849201', '-', '')), 
     250000.00);
-- Payment Service Initial Schema
CREATE TABLE IF NOT EXISTS `payments`
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

CREATE TABLE IF NOT EXISTS `credits`
(
    id               BINARY(16)     NOT NULL,
    customer_id      BINARY(16)     NOT NULL,
    amount           DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `uk_credits_customer_id` UNIQUE (customer_id)
);

CREATE TABLE IF NOT EXISTS `payment_outbox`
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

CREATE TABLE IF NOT EXISTS `payment_inbox`
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
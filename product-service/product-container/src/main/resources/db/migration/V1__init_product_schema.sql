-- Product Service Initial Schema
CREATE TABLE IF NOT EXISTS `products`
(
    id               BINARY(16)     NOT NULL,
    name             VARCHAR(255)   NOT NULL,
    price            DECIMAL(10,2)  NOT NULL,
    quantity         INT            NOT NULL,
    reserved_quantity INT           NOT NULL DEFAULT 0,
    enabled          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `product_reservations`
(
    id          BINARY(16)     NOT NULL,
    product_id  BINARY(16)     NOT NULL,
    order_id    BINARY(16)     NOT NULL,
    quantity    INT            NOT NULL,
    status      ENUM('PENDING', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP(6)   NOT NULL,
    updated_at  TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_product_reservations_products` FOREIGN KEY (product_id) REFERENCES `products` (id) ON DELETE CASCADE,
    INDEX `idx_product_reservations_product_id` (product_id),
    INDEX `idx_product_reservations_order_id` (order_id)
);

CREATE TABLE IF NOT EXISTS `product_outbox`
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
    CONSTRAINT `uk_product_outbox_message_id` UNIQUE (message_id),
    INDEX `idx_product_outbox_saga_id` (saga_id),
    INDEX `idx_product_outbox_fetch` (outbox_status, fetched_at)
);

CREATE TABLE IF NOT EXISTS `product_inbox`
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
    CONSTRAINT `uk_product_inbox_message_id` UNIQUE (message_id),
    INDEX `idx_product_inbox_saga_id` (saga_id),
    INDEX `idx_product_inbox_status_received` (status, received_at),
    INDEX `idx_product_inbox_status_retry_received` (status, retry_count, received_at)
);
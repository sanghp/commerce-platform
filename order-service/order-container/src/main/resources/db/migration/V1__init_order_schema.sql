-- Order Service Initial Schema
CREATE TABLE IF NOT EXISTS `orders`
(
    id BINARY(16) NOT NULL,
    customer_id BINARY(16) NOT NULL,
    tracking_id BINARY(16) NOT NULL,
    price decimal(10,2) NOT NULL,
    order_status ENUM('PENDING', 'RESERVED', 'PAID', 'CONFIRMED', 'CANCELLED', 'CANCELLING') NOT NULL,
    failure_messages TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    reserved_at TIMESTAMP(6),
    paid_at TIMESTAMP(6),
    confirmed_at TIMESTAMP(6),
    cancelling_at TIMESTAMP(6),
    cancelled_at TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT `uk_orders_tracking_id` UNIQUE (tracking_id)
);

CREATE TABLE IF NOT EXISTS `order_items`
(
    id BIGINT NOT NULL,
    order_id BINARY(16) NOT NULL,
    product_id BINARY(16) NOT NULL,
    price decimal(10,2) NOT NULL,
    quantity integer NOT NULL,
    sub_total decimal(10,2) NOT NULL,

    PRIMARY KEY (id, order_id),
    CONSTRAINT `fk_order_items_orders` FOREIGN KEY (order_id) REFERENCES `orders` (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `customers`
(
    id         BINARY(16)   NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `uk_customers_username` UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS `order_address`
(
    id BINARY(16) NOT NULL,
    order_id BINARY(16) UNIQUE NOT NULL,
    street varchar(255) NOT NULL,
    postal_code varchar(255) NOT NULL,
    city varchar(255) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT `fk_order_address_orders` FOREIGN KEY (order_id) REFERENCES `orders` (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `order_outbox`
(
    id BINARY(16) NOT NULL,
    message_id BINARY(16) NOT NULL,
    saga_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL,
    fetched_at DATETIME,
    processed_at DATETIME,
    type varchar(255) NOT NULL,
    payload JSON NOT NULL,
    outbox_status ENUM('STARTED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL,
    version integer NOT NULL,
    trace_id VARCHAR(32),
    span_id VARCHAR(16),

    PRIMARY KEY (id),
    CONSTRAINT `uk_order_outbox_message_id` UNIQUE (message_id),
    INDEX `idx_order_outbox_type_status` (type, outbox_status),
    INDEX `idx_order_outbox_saga_id` (saga_id),
    INDEX `idx_order_outbox_fetch` (outbox_status, fetched_at),
    INDEX `idx_order_outbox_created_at` (outbox_status, created_at)
);

CREATE TABLE IF NOT EXISTS `order_inbox`
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
    trace_id      VARCHAR(32),
    span_id       VARCHAR(16),

    PRIMARY KEY (id),
    CONSTRAINT `uk_order_inbox_message_id` UNIQUE (message_id),
    INDEX `idx_order_inbox_saga_id` (saga_id),
    INDEX `idx_order_inbox_status_received` (status, received_at),
    INDEX `idx_order_inbox_status_retry_received` (status, retry_count, received_at)
);
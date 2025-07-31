-- 외래 키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `order_address`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `order_outbox`;
DROP TABLE IF EXISTS `order_inbox`;

-- 외래 키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 테이블 생성
CREATE TABLE `orders`
(
    id BINARY(16) NOT NULL,
    customer_id BINARY(16) NOT NULL,
    tracking_id BINARY(16) NOT NULL,
    price decimal(10,2) NOT NULL,
    order_status ENUM('PENDING', 'RESERVED', 'PAID', 'CONFIRMED', 'CANCELLED', 'CANCELLING') NOT NULL,
    failure_messages TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE `order_items`
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

CREATE TABLE `order_address`
(
    id BINARY(16) NOT NULL,
    order_id BINARY(16) UNIQUE NOT NULL,
    street varchar(255) NOT NULL,
    postal_code varchar(255) NOT NULL,
    city varchar(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT `fk_order_address_orders` FOREIGN KEY (order_id) REFERENCES `orders` (id) ON DELETE CASCADE
);

CREATE TABLE `order_outbox`
(
    id BINARY(16) NOT NULL,
    saga_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL,
    fetched_at DATETIME,
    processed_at DATETIME,
    type varchar(255) NOT NULL,
    payload JSON NOT NULL,
    outbox_status ENUM('STARTED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL,
    version integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX `order_outbox_status`
    ON `order_outbox`
    (type, outbox_status);

CREATE INDEX `order_outbox_saga_id`
    ON `order_outbox`
    (saga_id);

CREATE INDEX `order_outbox_processing_timeout`
    ON `order_outbox`
    (outbox_status, fetched_at);

CREATE TABLE `order_inbox`
(
    id            BINARY(16)   NOT NULL,
    saga_id       BINARY(16)   NOT NULL,
    type          VARCHAR(255) NOT NULL,
    payload       JSON         NOT NULL,
    status        ENUM('RECEIVED', 'PROCESSED', 'FAILED') NOT NULL DEFAULT 'RECEIVED',
    received_at   TIMESTAMP(6) NOT NULL,
    processed_at  TIMESTAMP(6),
    retry_count   INT DEFAULT 0,
    error_message TEXT,
    version       INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT `uk_order_inbox_saga_id_type` UNIQUE (saga_id, type),
    INDEX `idx_status_received` (status, received_at),
    INDEX `idx_status_retry_received` (status, retry_count, received_at)
);

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `product_reservations`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `product_outbox`;
DROP TABLE IF EXISTS `product_inbox`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `products`
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

CREATE TABLE `product_reservations`
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

CREATE TABLE `product_outbox`
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

CREATE TABLE `product_inbox`
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

-- 초기 테스트 데이터 삽입
INSERT INTO `products` (id, name, price, quantity, reserved_quantity, enabled, created_at) VALUES
    (UNHEX(REPLACE('0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0', '-', '')), 
     '커피', 5000.00, 1000, 0, TRUE, NOW()),
    (UNHEX(REPLACE('1a2b3c4d-5e6f-7089-9a0b-cdef12345678', '-', '')), 
     '샌드위치', 8000.00, 500, 0, TRUE, NOW()),
    (UNHEX(REPLACE('2b3c4d5e-6f70-8192-a3b4-cdef56789012', '-', '')), 
     '샐러드', 12000.00, 300, 0, TRUE, NOW()),
    (UNHEX(REPLACE('3c4d5e6f-7081-92a3-b4c5-def678901234', '-', '')), 
     '피자', 25000.00, 200, 0, TRUE, NOW()),
    (UNHEX(REPLACE('4d5e6f70-8192-a3b4-c5d6-ef7890123456', '-', '')), 
     '파스타', 18000.00, 250, 0, TRUE, NOW());
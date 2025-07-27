-- 외래 키 체크 비활성화
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `order_address`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `payment_outbox`;
DROP TABLE IF EXISTS `product_reservation_outbox`;

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

CREATE TABLE `payment_outbox`
(
    id BINARY(16) NOT NULL,
    saga_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    type varchar(255) NOT NULL,
    payload JSON NOT NULL,
    outbox_status ENUM('STARTED', 'COMPLETED', 'FAILED') NOT NULL,
    saga_status ENUM('STARTED', 'FAILED', 'SUCCEEDED', 'PROCESSING', 'COMPENSATING', 'COMPENSATED') NOT NULL,
    order_status ENUM('PENDING', 'RESERVED', 'PAID', 'CONFIRMED', 'CANCELLED', 'CANCELLING') NOT NULL,
    version integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX `payment_outbox_saga_status`
    ON `payment_outbox`
    (type, outbox_status, saga_status);

CREATE TABLE `product_reservation_outbox`
(
    id BINARY(16) NOT NULL,
    saga_id BINARY(16) NOT NULL,
    created_at DATETIME NOT NULL,
    processed_at DATETIME,
    type varchar(255) NOT NULL,
    payload JSON NOT NULL,
    outbox_status ENUM('STARTED', 'COMPLETED', 'FAILED') NOT NULL,
    saga_status ENUM('STARTED', 'FAILED', 'SUCCEEDED', 'PROCESSING', 'COMPENSATING', 'COMPENSATED') NOT NULL,
    order_status ENUM('PENDING', 'RESERVED', 'PAID', 'CONFIRMED', 'CANCELLED', 'CANCELLING') NOT NULL,
    version integer NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX `product_reservation_outbox_saga_status`
    ON `product_reservation_outbox`
    (type, outbox_status, saga_status);

CREATE INDEX `product_reservation_outbox_saga_id_saga_status`
    ON `product_reservation_outbox`
    (saga_id, saga_status);

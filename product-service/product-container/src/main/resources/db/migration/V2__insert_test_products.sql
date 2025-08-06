-- 초기 테스트 상품 데이터 삽입
INSERT IGNORE INTO `products` (id, name, price, quantity, reserved_quantity, enabled, created_at) VALUES
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
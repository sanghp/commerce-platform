-- 초기 테스트 크레딧 데이터 삽입
INSERT IGNORE INTO `credits` (id, customer_id, amount) VALUES
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440001', '-', '')), 
     UNHEX(REPLACE('3fa85f64-5717-4562-b3fc-2c963f66afa6', '-', '')), 
     1000000.00),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440002', '-', '')), 
     UNHEX(REPLACE('123e4567-e89b-12d3-a456-426614174000', '-', '')), 
     500000.00),
    (UNHEX(REPLACE('550e8400-e29b-41d4-a716-446655440003', '-', '')), 
     UNHEX(REPLACE('987fcdeb-51a2-43d1-b4c5-316227849201', '-', '')), 
     250000.00);
-- 초기 테스트 고객 데이터 삽입
INSERT IGNORE INTO `customers` (id, username, first_name, last_name) VALUES
    (UNHEX(REPLACE('3fa85f64-5717-4562-b3fc-2c963f66afa6', '-', '')), 
     'user1', 'John', 'Doe'),
    (UNHEX(REPLACE('123e4567-e89b-12d3-a456-426614174000', '-', '')), 
     'user2', 'Jane', 'Smith'),
    (UNHEX(REPLACE('987fcdeb-51a2-43d1-b4c5-316227849201', '-', '')), 
     'user3', 'Bob', 'Johnson');
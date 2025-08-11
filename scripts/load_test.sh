#!/bin/bash

TOTAL_REQUESTS=100

echo "Starting load test with $TOTAL_REQUESTS requests at once..."
start_time=$(date +%s)

# 모든 요청을 한번에 백그라운드로 실행
for i in $(seq 1 $TOTAL_REQUESTS); do
    {
        curl -s -X 'POST' \
            'http://localhost:8080/api/v1/orders' \
            -H 'accept: */*' \
            -H 'Content-Type: application/json' \
            -d '{
                "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "price": 15000,
                "items": [
                    {
                        "productId": "0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0",
                        "quantity": 3,
                        "price": 5000
                    }
                ],
                "address": {
                    "street": "123 Main St",
                    "postalCode": "12345",
                    "city": "Seoul"
                  }
            }' > /dev/null 2>&1 && echo "Request $i completed"
    } &
done

# 모든 백그라운드 작업 완료 대기
wait

end_time=$(date +%s)
elapsed=$((end_time - start_time))
echo "Load test completed in ${elapsed} seconds!"

echo "Load test completed!"
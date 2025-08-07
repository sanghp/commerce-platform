# 🛒 Commerce Platform - MSA 기반 주문 처리 시스템

> ⚠️ **개발 진행 중**
>
> *   **구현 완료**: `Order Service`, `Product Service`, `Payment Service`
> *   **핵심 기능**: 주문-재고-결제 전체 플로우의 분산 트랜잭션 처리

Spring Boot 3.5.3과 Java 21을 기반으로 구축한 MSA 커머스 플랫폼의 주문 처리 시스템입니다.

이커머스의 핵심 플로우인 **주문-재고-결제** 프로세스에 집중하여 MSA 환경에서 발생하는 분산 트랜잭션 관리와 서비스 간 통신 문제를 해결하는데 중점을 두었습니다. 특히 Saga 패턴을 통한 트랜잭션 관리와 이벤트 기반 아키텍처로 서비스 간 느슨한 결합을 구현했습니다.

---

## 🏗️ 아키텍처
각 서비스는 `domain`, `application`, `dataaccess`, `messaging`, `container` 모듈로 구성되어 책임과 역할을 분리합니다.

*   **Domain**: 핵심 비즈니스 로직과 도메인 모델을 포함하며 다른 계층에 대한 의존성이 없는 순수한 비즈니스 규칙의 집합입니다.
*   **Application**: 외부와의 통신을 위한 REST API를 제공합니다.
*   **DataAccess**: 데이터 영속성을 처리하는 계층으로 Spring Data JPA를 사용하여 데이터베이스와 상호작용 합니다.
*   **Messaging**: Kafka 같은 메시지 브로커와의 통신을 담당하며 이벤트 발행 및 구독 로직을 포함합니다.
*   **Container**: 애플리케이션의 진입점(entrypoint) 역할을 합니다.

### 핵심 설계
- **DDD & Clean Architecture**: Dependency Rule에 따라 모든 종속성이 내부 계층으로 향하도록 설계했습니다. 이를 통해 외부 프레임워크나 인프라(DB, Messaging 등)의 변화로부터 핵심 비즈니스 로직을 보호하고 시스템의 유연성과 테스트 용이성을 확보합니다.
- **SAGA Pattern**: Orchestration 방식으로 분산 트랜잭션을 관리하며, Order Service가 전체 플로우의 조정자 역할을 수행합니다.
- **Outbox Pattern & Kafka**: 이벤트 발행시 로컬 트랜잭션으로 Outbox 테이블에 이벤트를 저장한 뒤 Kafka에 발행하여 Eventual Consistency를 구현합니다.
- **Inbox Pattern**: 메시지 중복 처리 방지를 위해 Inbox 패턴을 구현하여 이벤트 소비의 멱등성을 보장합니다.

---

## 🛠️ 기술 스택

| 분류 | 기술 | 버전 | 비고 |
| --- | --- | --- | --- |
| **Language** | Java | 21 | |
| **Framework** | Spring Boot | 3.5.3 | |
| **Build Tool** | Maven | 3.9.10 | Multi-module |
| **Persistence** | Spring Data JPA, MySQL | 8.0 | |
| **Messaging** | Confluent Platform (Apache Kafka) | 7.6.1 (Kafka 3.6.1) | KRaft mode |
| **Security** | Spring Security | | JWT 기반 인증 도입 예정 |
| **Architecture** | MSA, Clean, DDD, Hexagonal, SAGA | | |
| **Infrastructure** | Docker Compose | | |

---

## 🔄 주문 처리 흐름 (SAGA Pattern)

본 프로젝트는 주문 처리의 핵심 단계인 **주문 생성 → 재고 예약 → 결제 처리** 플로우를 구현합니다. **Saga 패턴(Orchestration)**을 통해 분산 트랜잭션을 관리하며 `Order Service`가 전체 흐름의 **Orchestrator** 역할을 수행합니다. 서비스 간 통신은 **Kafka를 통한 비동기 메시지 교환**으로 이루어지며 각 서비스는 `OrderStatus`를 기반으로 작업을 수행합니다.

1.  **주문 생성 및 재고 예약 요청**:
    *   `Order Service`는 주문을 `PENDING` 상태로 생성하고 Saga를 시작하기 위해 `product-reservation-request` 토픽으로 메시지를 발행합니다.
    *   이 메시지에는 `OrderStatus`가 `PENDING`으로 설정되어 있으며 `Product Service`는 해당 메시지를 구독하여 재고 예약을 시도합니다.

2.  **재고 예약 처리 및 응답 (Product Service)**:
    *   `Product Service`는 재고 예약 후, `product-reservation-response` 토픽으로 메세지를 발행합니다. 이 메시지에는 예약 상태를 나타내는 `ReservationStatus`가 포함됩니다.

3.  **결제 요청 (Order Service)**:
    *   `Order Service`는 재고 예약 성공 메세지를 구독한 뒤 `payment-request` 토픽으로 결제 요청 메시지를 발행합니다.

4.  **결제 처리 및 응답 (Payment Service)**:
    *   `Payment Service`는 결제 처리 후, `payment-response` 토픽으로 결과를 발행합니다. 이 메시지에는 결제 성공/실패 여부를 나타내는 `PaymentStatus`가 포함됩니다.

5.  **재고 확정 및 주문 완료**:
    *   `Order Service`는 결제 성공 메세지를 구독하면 주문 상태를 `PAID`로 변경합니다.
    *   이후 예약된 재고를 확정(차감)하기 위해 `product-reservation-request` 토픽으로 `OrderStatus`가 `BOOKED`로 설정된 메시지를 다시 발행합니다.
    *   `Product Service`는 해당 메시지를 구독하여 재고를 차감하고 `product-reservation-response` 토픽에 `OrderStatus`를 `CONFIRMED`로 설정하여 메세지를 발행합니다.
    *   `Order Service`가 해당 메세지를 구독하면 주문을 완료 처리하고 Saga 트랜잭션을 종료합니다.

6.  **보상 트랜잭션 (실패 처리)**:
    *   **재고 예약 실패 시**: `Product Service`는 재고 예약에 실패하면 `product-reservation-response` 토픽으로 `ReservationStatus`를 `REJECTED`로 설정하여 응답합니다. `Order Service`는 이 메시지를 구독하여 주문 상태를 `CANCELLED`로 변경하고 Saga를 종료합니다.
    *   **결제 실패 시**: `Payment Service`의 결제 실패 응답을 구독한 `Order Service`는 보상 트랜잭션을 시작합니다. `product-reservation-request` 토픽으로 `OrderStatus`를 `CANCELLED`로 설정하고 메시지를 발행하여 `Product Service`에 예약된 재고가 해제되도록 요청합니다. `Product Service`가 재고를 해제하고 `ReservationStatus`를 `CANCELLED`로 설정하여 메세지를 발행하면 `Order Service`는 주문의 상태를 최종적으로 `CANCELLED`로 변경하고 Saga를 종료합니다.

---

## 🚀 로컬 환경 통합 빌드 및 실행

로컬 환경에서 모든 서비스를 한 번에 빌드하고 실행 및 테스트할 수 있는 통합 스크립트를 제공합니다.

### 통합 빌드 및 실행 (`build-and-run.sh`)

아래 스크립트는 Maven 프로젝트 빌드, 컨테이너 이미지 생성, 인프라 및 애플리케이션 서비스 실행, 데이터베이스 마이그레이션까지 전 과정을 자동화합니다.

```bash
./scripts/build-and-run.sh
```

#### 실행 프로세스 상세

스크립트는 다음의 프로세스를 순차적으로 실행합니다.

1.  **빌드 & 이미지 생성**: `mvn clean install`를 실행하여 각 서비스를 컨테이너 이미지로 빌드합니다.
2.  **인프라 프로비저닝**: `docker-compose.yml`을 실행하여 `MySQL`, `Kafka`, `Schema Registry` 등 인프라 컨테이너를 실행합니다.
3.  **데이터베이스 마이그레이션**: `docker-compose-flyway.yml`을 통해 `Flyway`를 실행하여 각 서비스의 데이터베이스 스키마와 초기 데이터를 적용합니다.
4.  **서비스 오케스트레이션**: `docker-compose-services.yml`을 실행하여 모든 마이크로서비스(`Order`, `Product`, `Payment`) 컨테이너를 실행하고 서비스 간 네트워크를 구성합니다.
5.  **로드 밸런싱**: `HAProxy`가 각 서비스의 로드 밸런서 역할을 수행하며 외부 요청을 라우팅합니다. (`Order`:8080, `Product`:8090, `Payment`:8100)


### 분산 트랜잭션 부하 테스트 (`load_test.sh`)

Saga 패턴으로 구현된 분산 트랜잭션의 안정성과 성능을 검증하기 위한 부하 테스트 스크립트를 제공합니다.

```bash
./scripts/load_test.sh
```
#### 테스트 시나리오

*   **동시성 레벨**: 100개의 병렬 HTTP 요청을 통해 동시 주문 생성
*   **트랜잭션 유형**: **주문 생성 → 재고 예약 → 결제 처리**로 이어지는 전체 Saga 플로우
*   **주요 검증 항목**:
    *   **데이터 정합성**: Outbox/Inbox 패턴을 통한 메시지 멱등성 보장 및 중복 처리 방지
    *   **동시성 제어**: 다중 요청 환경에서 재고 및 잔액 데이터의 정확성 유지
    *   **보상 트랜잭션**: 재고 부족, 결제 실패 등 예외 상황 발생 시 보상 트랜잭션의 정상 동작 여부

#### 모니터링

테스트 실행 중 아래 엔드포인트를 통해 시스템 상태를 실시간으로 모니터링할 수 있습니다.

*   **Kafka UI**: `http://localhost:28080` - Kafka 토픽, 메시지 흐름 확인
*   **HAProxy Stats**: `http://localhost:9000/stats` - 서비스별 요청 분산 및 상태 확인

---

## 📖 편의 기능

애플리케이션 실행 후, 아래 링크를 통해 각 서비스의 API 문서를 확인하거나 Kafka UI에 접근할 수 있습니다.

- **Order Service API**: [http://localhost:8181/swagger-ui.html](http://localhost:8181/swagger-ui.html)
- **Product Service API**: [http://localhost:8182/swagger-ui.html](http://localhost:8182/swagger-ui.html)
- **Payment Service API**: [http://localhost:8183/swagger-ui.html](http://localhost:8183/swagger-ui.html)
- **Kafka UI**: [http://localhost:28080](http://localhost:28080)

---

## 📖 API 사용 예시

> [!NOTE]
> **초기 데이터로 다음 상품들이 등록되어 있습니다:**
> - `0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0`: 커피 (5,000원, 재고 1000개)
> - `1a2b3c4d-5e6f-7089-9a0b-cdef12345678`: 샌드위치 (8,000원, 재고 500개)
> - `2b3c4d5e-6f70-8192-a3b4-cdef56789012`: 샐러드 (12,000원, 재고 300개)
> - `3c4d5e6f-7081-92a3-b4c5-def678901234`: 피자 (25,000원, 재고 200개)
> - `4d5e6f70-8192-a3b4-c5d6-ef7890123456`: 파스타 (18,000원, 재고 250개)

### Order Service (http://localhost:8181)

#### 1. 주문 생성

- **POST** `/api/v1/orders`

커피 3개를 주문하는 예시:

```bash
curl -X 'POST' \
  'http://localhost:8181/api/v1/orders' \
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
}'
```

피자 1개와 파스타 2개를 주문하는 예시:

```bash
curl -X 'POST' \
  'http://localhost:8181/api/v1/orders' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "price": 61000,
  "items": [
    {
      "productId": "3c4d5e6f-7081-92a3-b4c5-def678901234",
      "quantity": 1,
      "price": 25000
    },
    {
      "productId": "4d5e6f70-8192-a3b4-c5d6-ef7890123456",
      "quantity": 2,
      "price": 18000
    }
  ],
  "address": {
    "street": "456 Oak St",
    "postalCode": "67890",
    "city": "Busan"
  }
}'
```

성공 시 주문 추적을 위한 `orderTrackingId`가 반환됩니다.

#### 2. 주문 조회

- **GET** `/api/v1/orders/{orderTrackingId}`

```bash
curl -X GET http://localhost:8181/api/v1/orders/위에서-받은-orderTrackingId
```



---

## 📌 향후 개선 계획
-   **고객 인증/인가 시스템**: JWT 기반 인증 시스템을 구축하여 실제 회원만 주문 가능하도록 개선
-   **CDC(Change Data Capture) 도입**: Debezium을 활용하여 Outbox 패턴을 CDC 기반 이벤트 발행으로 전환합니다. 이를 통해 애플리케이션의 비즈니스 로직과 이벤트 발행 메커니즘을 완전히 분리하여 결합도를 낮출 수 있습니다.
-   **모니터링 및 추적**: OpenTelemetry, Prometheus, Grafana를 활용한 분산 추적 및 모니터링 시스템 구축
-   **테스트 커버리지 향상**: 단위 테스트, 통합 테스트, E2E 테스트 작성
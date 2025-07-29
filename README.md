# 🛒 Commerce Platform - MSA 기반 주문 처리 시스템

> ⚠️ **개발 진행 중**
>
> *   **구현 완료**: `Order Service`, `Product Service`  
> *   **핵심 기능**: 주문 생성부터 재고 예약까지의 분산 트랜잭션 처리
> *   **구현 예정**: `Payment Service`, `Customer Service`

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
- **CQRS**: 명령(Command)과 조회(Query)의 책임을 분리하여 시스템의 성능과 확장성을 향상시킵니다. `Order Service`에서 발생한 주문 완료 이벤트를 `Customer Service`가 구독하여 조회에 최적화된 별도의 **`주문 내역` 테이블을 유지합니다.** 이를 통해 사용자는 자신의 주문 내역을 빠르고 안정적으로 조회할 수 있으며 쓰기(Write)와 읽기(Read) 모델의 관심사를 분리하여 각 서비스의 독립성을 높입니다.
- **Outbox Pattern & Kafka**: 이벤트 발행시 로컬 트랜잭션으로 Outbox 테이블에 이벤트를 저장한 뒤 Kafka에 발행하여 Eventual Consistency를 구현합니다.

---

## 🛠️ 기술 스택

| 분류 | 기술 | 버전 | 비고 |
| --- | --- | --- | --- |
| **Language** | Java | 21 | |
| **Framework** | Spring Boot | 3.5.3 | |
| **Build Tool** | Maven | 3.9.10 | Multi-module |
| **Persistence** | Spring Data JPA, MySQL | 8.0 | |
| **Messaging** | Confluent Platform (Apache Kafka) | 7.6.1 (Kafka 3.6.1) | KRaft mode |
| **Security** | Spring Security, JWT | | |
| **Architecture** | MSA, Clean, DDD, Hexagonal, CQRS | | |
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

4.  **결제 처리 및 응답 (Payment Service) `(구현 예정)`**:
    *   `Payment Service`는 결제 처리 후, `payment-response` 토픽으로 결과를 발행합니다. 이 메시지에는 결제 성공/실패 여부를 나타내는 `PaymentStatus`가 포함됩니다.

5.  **재고 확정 및 주문 완료 `(구현 예정)`**:
    *   `Order Service`는 결제 성공 메세지를 구독하면 주문 상태를 `PAID`로 변경합니다.
    *   이후 예약된 재고를 확정(차감)하기 위해 `product-reservation-request` 토픽으로 `OrderStatus`가 `BOOKED`로 설정된 메시지를 다시 발행합니다.
    *   `Product Service`는 해당 메시지를 구독하여 재고를 차감하고 `product-reservation-response` 토픽에 `OrderStatus`를 `CONFIRMED`로 설정하여 메세지를 발행합니다.
    *   `Order Service`가 해당 메세지를 구독하면 주문을 완료 처리하고 Saga 트랜잭션을 종료합니다.

6.  **보상 트랜잭션 (실패 처리) `(일부 구현)`**:
    *   **재고 예약 실패 시**: `Product Service`는 재고 예약에 실패하면 `product-reservation-response` 토픽으로 `ReservationStatus`를 `REJECTED`로 설정하여 응답합니다. `Order Service`는 이 메시지를 구독하여 주문 상태를 `CANCELLED`로 변경하고 Saga를 종료합니다.
    *   **결제 실패 시 `(구현 예정)`**: `Payment Service`의 결제 실패 응답을 구독한 `Order Service`는 보상 트랜잭션을 시작합니다. `product-reservation-request` 토픽으로 `OrderStatus`를 `CANCELLED`로 설정하고 메시지를 발행하여 `Product Service`에 예약된 재고가 해제되도록 요청합니다. `Product Service`가 재고를 해제하고 `ReservationStatus`를 `CANCELLED`로 설정하여 메세지를 발행하면 `Order Service`는 주문의 상태를 최종적으로 `CANCELLED`로 변경하고 Saga를 종료합니다.

---

## ⚙️ 실행 방법

모든 명령어는 **프로젝트 루트 디렉터리**를 기준으로 실행합니다.

### 1. 애플리케이션 빌드

먼저 프로젝트 루트 디렉터리에서 다음 명령어를 실행하여 모든 서비스 모듈을 빌드합니다.

```bash
./mvnw clean install
```

### 2. 인프라 실행 (Kafka, MySQL)

다음 명령어로 모든 인프라 서비스(MySQL, Kafka, Schema Registry)를 백그라운드에서 실행합니다.

```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml up -d
```

### 3. 서비스 실행

각 마이크로서비스는 개별적으로 실행해야 합니다. **각 서비스마다 새 터미널을 열고** 아래 명령어를 실행하세요.

| 서비스             | 실행 명령어                                                                                       |
| ------------------ |----------------------------------------------------------------------------------------------|
| **Order Service**  | `java -jar order-service/order-container/target/order-container-0.0.1-SNAPSHOT.jar`          |
| **Product Service**| `java -jar product-service/product-container/target/product-container-0.0.1-SNAPSHOT.jar`    |

---

## 📖 편의 기능

애플리케이션 실행 후, 아래 링크를 통해 각 서비스의 API 문서를 확인하거나 Kafka UI에 접근할 수 있습니다.

- **Order Service API**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Product Service API**: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)
- **Kafka UI**: [http://localhost:28080](http://localhost:28080)

---

## 📖 API 사용 예시

### Product Service (http://localhost:8082)

#### 1. 상품 생성

- **POST** `/api/v1/products`

```bash
curl -X 'POST' \
  'http://localhost:8182/api/v1/products' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "price": 5000,
  "quantity": 15
}'
```

성공 시 생성된 상품의 ID가 반환됩니다.

### Order Service (http://localhost:8081)

#### 1. 주문 생성

- **POST** `/api/v1/orders`

> [!NOTE]
> `customerId`는 임의의 UUID를 사용하고, `productId`는 위에서 생성된 상품의 ID를 입력해야 합니다.

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
      "productId": "8baf82ad-d492-4021-a349-be517e5d181a",
      "quantity": 3,
      "price": 5000
    }
  ],
  "address": {
    "street": "123 Main St",
    "postalCode": "12345",
    "city": "Anytown"
  }
}'
```

성공 시 주문 추적을 위한 `orderTrackingId`가 반환됩니다.

#### 2. 주문 조회

- **GET** `/api/v1/orders/{orderTrackingId}`

```bash
curl -X GET http://localhost:8081/api/v1/orders/위에서-받은-orderTrackingId
```

## 🐳 docker-compose.yml

```yaml
version: '3.8'

x-kafka-common-env: &kafka-common-env
  KAFKA_PROCESS_ROLES: 'broker,controller'
  KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
  KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT'
  KAFKA_INTER_BROKER_LISTENER_NAME: 'INTERNAL'
  KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka1:9093,2@kafka2:9093,3@kafka3:9093'
  CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
  KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
  KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
  KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
  KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "13306:3306"

  schema-registry:
    image: confluentinc/cp-schema-registry:7.6.1
    hostname: schema-registry
    depends_on:
      - kafka1
      - kafka2
      - kafka3
    ports:
      - "18081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: 'kafka1:29092,kafka2:29092,kafka3:29092'
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081

  kafka1:
    image: confluentinc/cp-kafka:7.6.1
    hostname: kafka1
    ports:
      - "19092:9092"
    environment:
      <<: *kafka-common-env
      KAFKA_NODE_ID: 1
      KAFKA_LISTENERS: 'INTERNAL://0.0.0.0:29092,CONTROLLER://kafka1:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'INTERNAL://kafka1:29092,EXTERNAL://localhost:19092'

  kafka2:
    image: confluentinc/cp-kafka:7.6.1
    hostname: kafka2
    ports:
      - "19093:9092"
    environment:
      <<: *kafka-common-env
      KAFKA_NODE_ID: 2
      KAFKA_LISTENERS: 'INTERNAL://0.0.0.0:29092,CONTROLLER://kafka2:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'INTERNAL://kafka2:29092,EXTERNAL://localhost:19093'

  kafka3:
    image: confluentinc/cp-kafka:7.6.1
    hostname: kafka3
    ports:
      - "19094:9092"
    environment:
      <<: *kafka-common-env
      KAFKA_NODE_ID: 3
      KAFKA_LISTENERS: 'INTERNAL://0.0.0.0:29092,CONTROLLER://kafka3:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'INTERNAL://kafka3:29092,EXTERNAL://localhost:19094'

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "28080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka1:29092,kafka2:29092,kafka3:29092
      KAFKA_CLUSTERS_0_SCHEMAREGISTRY: http://schema-registry:8081
    depends_on:
      - kafka1
      - kafka2
      - kafka3
      - schema-registry 
```

---

## 📌 향후 개선 계획
-   **CDC(Change Data Capture) 도입**: Debezium을 활용하여 Outbox 패턴을 CDC 기반 이벤트 발행으로 전환합니다. 이를 통해 애플리케이션의 비즈니스 로직과 이벤트 발행 메커니즘을 완전히 분리하여 결합도를 낮출 수 있습니다.
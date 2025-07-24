# 🛒 Commerce Platform

Spring Boot 3.5.3, Java 21 기반으로 구축한 MSA 기반 커머스 플랫폼 백엔드 서비스입니다.
본 프로젝트는 대규모 트래픽을 처리하는 커머스 환경을 가정하여 MSA 환경에서 확장성과 데이터 일관성을 확보하는 것을 목표로 설계되었습니다.

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

## 🔄 주문 및 재고 처리 흐름 (SAGA Pattern)

해당 프로젝트의 핵심 비즈니스 로직은 **Saga 패턴(Orchestration)**을 통해 구현됩니다. `Order Service`가 전체 트랜잭션의 흐름을 지휘하는 **Orchestrator** 역할을 수행하며 모든 서비스 간의 통신은 **Kafka를 통한 비동기 메시지 교환**으로 이루어집니다.

1.  **주문 생성 및 재고 예약 요청**:
    *   `Order Service`는 주문을 `PENDING` 상태로 생성하고 재고 예약을 위해 `Product Service`로 `ProductReservationRequest` 이벤트를 Kafka로 발행하며 Saga를 시작합니다.

2.  **재고 예약 처리 및 응답 (Product Service)**:
    *   `Product Service`는 `ProductReservationRequest`를 구독하여 재고 예약을 시도합니다.
    *   처리 결과를 `ProductReservationResponse` 이벤트(성공/실패 여부 포함)로 발행합니다.

3.  **결제 요청 (Order Service)**:
    *   `Order Service`는 `ProductReservationResponse`(성공) 이벤트를 구독한 뒤 결제를 위해 `Payment Service`로 `PaymentRequest` 이벤트를 발행합니다.

4.  **결제 처리 및 응답 (Payment Service)**:
    *   `Payment Service`는 `PaymentRequest`를 구독하여 결제를 시도합니다.
    *   처리 결과를 `PaymentResponse` 이벤트(성공/실패 여부 포함)로 발행합니다.

5.  **주문 완료 및 재고 최종 차감**:
    *   `Order Service`는 `PaymentResponse`(성공) 이벤트를 구독하면 비즈니스 관점에서 주문이 완료되었으므로 주문 상태를 `COMPLETED`로 변경하고 Saga 트랜잭션을 종료하는 최종 `OrderPaidEvent`를 발행합니다.
        *   *Note: 결제가 성공한 시점에 이미 재고는 '예약' 상태이므로 고객에게 상품을 제공할 의무가 발생합니다. 따라서 `Order Service`는 자신의 책임을 다한 것으로 간주합니다.*
    *   `Product Service`는 `OrderPaidEvent`를 구독하여 예약 상태였던 재고를 실제 '차감' 상태로 갱신하는 후처리 작업을 수행합니다. 이 작업은 재시도(Retry) 및 DLQ(Dead-Letter Queue)를 통해 최종적인 데이터 정합성을 보장합니다.

6.  **보상 트랜잭션 (실패 처리)**:
    *   **재고 예약 실패 시**: `ProductReservationResponse`(실패) 이벤트를 수신하면 `Order Service`는 아직 다른 서비스에 변경을 가한 사항이 없으므로 생성했던 주문의 상태를 `CANCELLED`로 변경하고 Saga를 실패로 종료합니다.
    *   **결제 실패 시**: `PaymentResponse`(실패) 이벤트를 수신하면, `Order Service`는 이전에 성공했던 '재고 예약'을 취소하기 위한 보상 트랜잭션을 시작합니다. `Product Service`로 `ProductReservationCancelRequest`를 발행하여 예약된 재고를 해제하여 데이터 일관성을 맞춥니다.

---

## ⚙️ 실행 방법

### 1. 인프라 실행 (Kafka, MySQL)
```bash
docker-compose up -d
```

### 2. 프로젝트 빌드 및 실행
```bash
./mvnw clean install
# 각 서비스 모듈 실행
java -jar order-service/order-container/target/order-service-0.0.1-SNAPSHOT.jar
java -jar payment-service/payment-container/target/payment-service-0.0.1-SNAPSHOT.jar
java -jar product-service/product-container/target/product-service-0.0.1-SNAPSHOT.jar
java -jar customer-service/customer-container/target/customer-service-0.0.1-SNAPSHOT.jar
```

---

## 🐳 docker-compose.yml

```yaml
version: '3.8'

x-kafka-common-env: &kafka-common-env
  KAFKA_PROCESS_ROLES: 'broker,controller'
  KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
  KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,EXTERNAL:PLAINTEXT'
  KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
  KAFKA_CONTROLLER_QUORUM_VOTERS: '1@kafka1:9093,2@kafka2:9093,3@kafka3:9093'
  CLUSTER_ID: 'MkU3OEVBNTcwNTJENDM2Qk'
  KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
  KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
  KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: commerce
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
      KAFKA_LISTENERS: 'PLAINTEXT://kafka1:29092,CONTROLLER://kafka1:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka1:29092,EXTERNAL://localhost:19092'

  kafka2:
    image: confluentinc/cp-kafka:7.6.1
    hostname: kafka2
    ports:
      - "19093:9092"
    environment:
      <<: *kafka-common-env
      KAFKA_NODE_ID: 2
      KAFKA_LISTENERS: 'PLAINTEXT://kafka2:29092,CONTROLLER://kafka2:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka2:29092,EXTERNAL://localhost:19093'

  kafka3:
    image: confluentinc/cp-kafka:7.6.1
    hostname: kafka3
    ports:
      - "19094:9092"
    environment:
      <<: *kafka-common-env
      KAFKA_NODE_ID: 3
      KAFKA_LISTENERS: 'PLAINTEXT://kafka3:29092,CONTROLLER://kafka3:9093,EXTERNAL://0.0.0.0:9092'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://kafka3:29092,EXTERNAL://localhost:19094'
```

---

## 📌 향후 개선 계획
-   **CDC(Change Data Capture) 도입**: Debezium을 활용하여 Outbox 패턴을 CDC 기반 이벤트 발행으로 전환합니다. 이를 통해 애플리케이션의 비즈니스 로직과 이벤트 발행 메커니즘을 완전히 분리하여 결합도를 낮출 수 있습니다.
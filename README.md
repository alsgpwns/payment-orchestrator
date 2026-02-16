# Payment Orchestrator

High-availability payment authorization orchestration platform with idempotency, event-driven state management, and admin console.

---

## Overview

Payment Orchestrator는 가맹점과 PG/VAN 사이에서 결제 승인 요청을 처리하는 트랜잭션 오케스트레이션 플랫폼입니다.
실무에서 자주 마주치는 문제를 해결하기 위한 구조를 설계했습니다.

- 멱등성(Idempotency)을 통한 중복 승인 방지
- 이벤트 기반 상태 관리(State + Event Log)
- 거래 타임라인 추적(Event Log)
- 운영 가시성(Admin Console)
- PostgreSQL 기반 정합성 보장

---

## Architecture

Merchant → Payment Orchestrator (Spring Boot) → PostgreSQL (Transaction + Event Log)  
                                                        └→ Redis (Idempotency Key)

---

## Tech Stack

- Java 21
- Spring Boot 4.0.2
- Spring Data JPA
- PostgreSQL
- Redis
- Docker / Docker Compose
- Thymeleaf (Admin UI)

---

## How to Run

### 1) Start Infrastructure

```bash
docker compose up -d
```

PostgreSQL: localhost:5432  
Redis: localhost:6379  

---

### 2) Run Application

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `PaymentOrchestratorApplication` 실행

---

## API Example

### Authorize Payment

```powershell
$headers = @{ "Idempotency-Key" = "demo-1234" }

$body = @{
  merchantId = "M001"
  amount     = 12000
  currency   = "KRW"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/payments/authorize" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $body
```

---

## Idempotency Behavior

동일한 `Idempotency-Key`로 요청하면  
동일한 `txId`와 동일한 응답을 반환합니다.

중복 승인 방지를 위해 DB Unique Key 기반 멱등성을 구현했습니다.

---

## Admin Console

```
http://localhost:8080/admin/transactions
```

제공 기능:

- 거래 목록 조회
- 거래 상세 조회
- 이벤트 타임라인 확인

---

## Database Schema

### transactions

- id (UUID)
- merchant_id
- amount
- currency
- status
- idempotency_key
- created_at
- updated_at

### tx_events

- id
- tx_id
- event_type
- message
- created_at

---

## Design Highlights

### 1. Idempotency Control
- Header 기반 Idempotency-Key 처리
- DB Unique Constraint 기반 중복 방지

### 2. Event-Driven State Management
- 거래 생성 시 이벤트 로그 기록
- 상태 변경 이력 추적 가능

### 3. Operational Visibility
- 운영 콘솔 제공
- 거래 타임라인 확인 가능

---

## Next Steps

- PG Mock 서버 추가
- Failover / Circuit Breaker 구현
- 상태 머신 고도화
- Observability (Metrics / Trace) 확장

---

## Why This Project

실제 금융 트랜잭션 환경에서 중요한

- 정합성
- 멱등성
- 상태 전이 관리
- 운영 가시성

을 설계 관점에서 구현하기 위한 사이드 프로젝트입니다.


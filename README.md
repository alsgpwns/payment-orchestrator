# Payment Orchestrator

Idempotency-Key 기반 멱등 승인 처리와 상태전이(TxStatus) + 이벤트 로그(TxEvent) 타임라인을 제공하는 결제 승인 오케스트레이터 MVP.

---

## Overview

Payment Orchestrator는 가맹점의 승인 요청을 받아 외부 PG로 라우팅하고, 결과를 트랜잭션 상태와 이벤트 로그로 남겨 운영 관점에서 추적 가능하게 만드는 프로젝트입니다.

구현 포인트:

* Idempotency-Key 기반 중복 승인 방지(DB Unique + 조회 기반)
* TxStatus 상태전이 및 주요 단계별 TxEvent 기록(CREATED → ROUTED_TO_EXTERNAL → EXTERNAL_RESPONSE → AUTHORIZED/FAILED)
* Admin Console(Thymeleaf)에서 승인 요청/결과/거래 상세/이벤트 타임라인까지 웹으로 확인
* pg-mock으로 승인/거절 시나리오를 재현 가능하게 구성

---

## Architecture (Local)

Merchant/Admin UI
→ payment-orchestrator (Spring Boot, 8080)
→ PostgreSQL (transactions + tx_events)
→ pg-mock (Spring Boot, 8081)

---

## Tech Stack

* Java 21
* Spring Boot / Spring MVC
* Spring Data JPA
* PostgreSQL
* Redis (연동 준비)
* Thymeleaf (Admin UI)
* Gradle

---

## How to Run

### 1) Run payment-orchestrator

```bash
./gradlew bootRun
```

### 2) Run pg-mock (separate module)

```bash
cd pg-mock
./gradlew bootRun
```

Ports:

* Orchestrator: [http://localhost:8080](http://localhost:8080)
* PG Mock: [http://localhost:8081](http://localhost:8081)

---

## API Example

### Authorize Payment

```powershell
$headers = @{ "Idempotency-Key" = "demo-1234" }

$body = @{
  merchantId = "M001"
  amount     = 9000
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

- 동일한 `Idempotency-Key`로 요청하면 기존 거래를 조회해 동일한 `txId`와 동일한 응답을 반환합니다.
- 따라서 같은 키로 `amount` 등 요청 값을 변경해도 최초 생성된 거래가 우선됩니다(중복 승인 방지 목적의 정책).

---

## Admin Console

* 승인 요청 폼:
  [http://localhost:8080/admin/authorize](http://localhost:8080/admin/authorize)
* 거래 목록:
  [http://localhost:8080/admin/transactions](http://localhost:8080/admin/transactions)
* 거래 상세(타임라인):
  [http://localhost:8080/admin/transactions/{txId}](http://localhost:8080/admin/transactions/{txId})

제공 기능:

* 승인 요청 → 결과 화면(tx + events 타임라인)
* 거래 목록에서 상태/PG 결과 코드/승인번호/실패사유 확인(포폴 캡쳐 포인트)
* 거래 상세에서 이벤트 타임라인으로 단계별 근거 추적

---

## Database Schema (MVP)

### transactions

* id (UUID)
* merchant_id, amount, currency
* status
* idempotency_key (unique)
* selected_pg, pg_result_code, approval_no, fail_reason
* routed_at, authorized_at
* created_at, updated_at

### tx_events

* id
* tx_id
* event_type
* message
* created_at

---

## Design Highlights

### 1) Idempotency (Duplicate Authorization Prevention)

* Header 기반 Idempotency-Key
* DB unique + 조회 기반으로 중복 거래 생성 방지

### 2) State Transition + Audit Trail

* TxStatus로 현재 상태를 표현
* TxEvent로 상태 변화의 근거 로그를 남겨 운영/장애 분석에 활용 가능

### 3) Operational Visibility

* Admin UI로 “터미널 없이” 승인 시나리오 재현 및 결과/타임라인 확인 가능

---

## Next Steps

* Redis 기반 Idempotency 저장/TTL 정책 확장(선택)
* 외부 연동 장애 대응(Timeout/Retry/Backoff, Circuit Breaker)
* Observability: metrics/log correlation, tracing 확장
* 상태 머신/정책(같은 키 + 다른 payload 처리) 고도화

---

## Why This Project

실제 결제/금융 트랜잭션에서 중요한

* 정합성(중복 승인 방지)
* 상태 전이 관리
* 운영 가시성(근거 로그/타임라인)

을 “운영 가능한 형태”로 구현하는 것을 목표로 했습니다.

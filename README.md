# fisa-card-company
## FISA Eureka Server

MSA 환경에서 서비스 디스커버리를 담당하는 Eureka Server입니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.12
- Spring Cloud Netflix Eureka Server

## 서비스 포트

| 서비스 | 포트 |
|--------|------|
| Eureka Server | 8761 |

## 주요 기능

- 서비스 등록 및 관리
- 서비스 상태 모니터링
- 대시보드 제공

## 대시보드
```
http://localhost:8761
```

## 등록된 서비스

| 서비스 | 포트 |
|--------|------|
| GATEWAY | 8080 |
| VAN | 8081 |
| PAYMENT-SERVICE | 8082 |
| BANKING-SERVICE | 8083 |
| SETTLEMENT-SERVICE | 8084 |

## 실행 방법

### Docker 실행
```bash
docker-compose up -d eureka-server
```

### 로컬 실행
```bash
./gradlew bootRun
```

## 실행 순서

> Eureka Server는 반드시 **가장 먼저** 실행해야 합니다.
> 다른 서비스들이 Eureka에 등록되어야 정상 동작합니다.

## FISA API Gateway

카드사 시스템의 진입점 역할을 하는 API Gateway입니다.
VAN으로부터 요청을 수신하여 내부 서비스로 라우팅합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.12
- Spring Cloud Gateway (MVC)
- Eureka Client

## 서비스 포트

| 서비스 | 포트 |
|--------|------|
| API Gateway | 8080 |

## 주요 기능

- 요청 라우팅
- TraceId 생성 및 로깅
- Eureka 서비스 등록

## 라우팅 규칙

| 경로 | 대상 서비스 | 포트 |
|------|------------|------|
| `/api/payment/**` | Payment Service | 8082 |
| `/payment/**` | Payment Service | 8082 |
| `/api/settlement/**` | Settlement Service | 8084 |

## TraceId

모든 요청에 `X-Trace-Id` 헤더를 자동으로 생성합니다.
MSA 환경에서 요청 흐름을 추적하는 데 사용됩니다.
```
[GATEWAY] TraceId: a1b2c3d4e5f6g7h8 | POST /api/payment/approve
```

## 실행 방법

### Docker 실행
```bash
docker-compose up -d api-gateway
```

### 로컬 실행
```bash
./gradlew bootRun
```

## 실행 순서

1. Eureka Server 먼저 실행
2. API Gateway 실행
3. 내부 서비스들 실행

## 전체 시스템 흐름
```
VAN(8081) → API Gateway(8080) → Payment Service(8082) → Banking Service(8083)
                               → Settlement Service(8084) → Banking Service(8083)
```

---

## FISA Banking Service

은행 시스템을 시뮬레이션하는 서비스입니다.
계좌 잔액 조회, 출금(체크카드 승인 시), 정산 이체(D+1)를 처리합니다.

## 기술 스택

- Java 17
- Spring Boot 3.5.12
- Spring Data JPA
- Eureka Client

## 서비스 포트

| 서비스 | 포트 |
|--------|------|
| Banking Service | 8083 |

## 데이터베이스

| DB명 | 스키마 | 포트 |
|------|--------|------|
| bank_system | bank_accounts, bank_transfer_log | 3314 |

## 주요 기능

- 계좌 잔액 조회
- 출금 처리 (체크카드 결제 시 사용자 계좌 → 카드사 계좌)
- 정산 이체 (D+1 배치 시 카드사 계좌 → 가맹점 계좌)
- 이체 기록 관리

## API 엔드포인트

### 잔액 조회

`POST /api/bank/balance`

```json
// Request
{ "accountNo": "1001-0001" }

// Response (200)
{ "accountNo": "1001-0001", "balance": 1000000, "ownerName": "홍길동" }
```

### 출금 (체크카드 승인)

`POST /api/bank/withdraw`

Payment Service에서 체크카드 결제 시 호출합니다.
사용자 연결 계좌에서 카드사 계좌로 결제 금액을 이체합니다.

```json
// Request
{ "fromAccount": "1001-0001", "toAccount": "9000-0001", "amount": 50000 }

// Response - 성공 (200)
{ "success": true, "message": "출금 완료", "transferId": 1 }

// Response - 잔액 부족 (400)
{ "success": false, "code": "INSUFFICIENT_BALANCE", "message": "잔액이 부족합니다: 1001-0001" }
```

### 정산 이체 (D+1 배치)

`POST /api/bank/transfer`

Settlement Service에서 D+1 정산 시 호출합니다.
카드사 계좌에서 가맹점 계좌로 수수료 차감 후 정산금을 이체합니다.

```json
// Request
{ "fromAccount": "9000-0001", "toAccount": "2001-0001", "amount": 49250 }

// Response (200)
{ "success": true, "message": "이체 완료", "transferId": 2 }
```

## 테스트 데이터 (bank_accounts)

| 계좌번호 | 소유자 | 잔액 | 유형 |
|----------|--------|------|------|
| 1001-0001 | 홍길동 | 1,000,000 | USER |
| 1001-0002 | 김철수 | 500,000 | USER |
| 9000-0001 | 우리카드 | 1,000,000,000 | CARD_COMPANY |
| 9000-0002 | VAN사 | 0 | CARD_COMPANY |
| 2001-0001 | 스타벅스 | 0 | MERCHANT |
| 2001-0002 | CU편의점 | 0 | MERCHANT |

## 실행 방법

### Docker 실행
```bash
docker-compose up -d banking-service
```

### 로컬 실행
```bash
cd banking-service
./gradlew bootRun
```

---

## Payment Service - 체크카드 결제

Payment Service 내 체크카드 결제 승인 처리 로직입니다.
신용카드와 달리 은행 서버에 실시간 출금 요청을 보내 잔액을 확인합니다.

## 결제 흐름

```
VAN → Gateway → PaymentController(/api/payment/approve)
                        │
                        ├─ 카드 타입 조회 (CardMasterService)
                        │
                        ├─ CREDIT → CreditPaymentService (한도 차감, 은행 호출 없음)
                        │
                        └─ CHECK  → CheckPaymentService
                                        │
                                        ├─ 1. RRN 생성 (12자리 HEX)
                                        ├─ 2. 카드 검증 (shared DB)
                                        ├─ 3. 은행 출금 요청 (BankClient → Banking Service)
                                        │      사용자 계좌 → 카드사 계좌(9000-0001)
                                        ├─ 4. 원장 기록 (source DB)
                                        └─ 5. 응답 반환
```

## 신용카드 vs 체크카드

| 항목 | 신용카드 | 체크카드 |
|------|---------|---------|
| 승인 방식 | 카드사 내부 한도 차감 | 은행 계좌에서 실시간 출금 |
| 은행 호출 | 없음 | Banking Service에 출금 요청 |
| 잔액/한도 확인 | card_master.credit_limit | bank_accounts.balance |
| 동시성 제어 | Pessimistic Lock (한도 차감) | 락 불필요 (card_master 수정 없음) |

## 체크카드 요청/응답

`POST /api/payment/approve`

```json
// Request
{
  "cardNumber": "1234567890120003",
  "amount": 50000,
  "merchantId": "MERCHANT_001",
  "stan": "123456"
}

// Response - 승인 (200)
{
  "rrn": "4CCCD3168CA8",
  "approvalCode": "025383",
  "responseCode": "00",
  "status": "APPROVED",
  "message": "승인 완료"
}

// Response - 거절 (200)
{
  "rrn": "B3F88D95281F",
  "responseCode": "99",
  "status": "REJECTED",
  "message": "계좌 잔액이 부족합니다."
}
```

## 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| PAY-010 | 400 | 체크카드 전용 엔드포인트입니다. |
| PAY-011 | 400 | 계좌 잔액이 부족합니다. |
| PAY-012 | 500 | 은행 서버 호출에 실패했습니다. |

## 서비스 간 통신

Banking Service와의 통신은 `BankClient`가 RestTemplate으로 처리합니다.

```
CheckPaymentService
  └─ BankClient.withdraw(fromAccount, toAccount, amount)
       └─ POST http://banking-service:8083/api/bank/withdraw
            ├─ 성공 → true (잔액 차감 완료)
            ├─ 400 에러 → false (잔액 부족 등)
            └─ 통신 실패 → PaymentException (PAY-012)
```

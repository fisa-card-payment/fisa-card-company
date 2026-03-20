# 찬혁 파트 구현 현황

## 현재 구현 완료된 것: banking-service (은행 서버)

### 개요
카드사 시스템 내부의 은행 서버(서버 3)를 구현했다.
체크카드 승인 시 잔액 확인/출금, 정산 시 가맹점 이체를 담당하는 서버다.

- 포트: 8083
- DB: bank_system (localhost:3314)
- 단일 DataSource, Spring Data JPA

---

### 생성한 파일 목록

```
banking-service/src/main/java/dev/bank/
├── entity/
│   ├── BankAccount.java           -- bank_accounts 테이블 매핑
│   └── BankTransferLog.java       -- bank_transfer_log 테이블 매핑
├── repository/
│   ├── BankAccountRepository.java
│   └── BankTransferLogRepository.java
├── dto/
│   ├── BalanceRequest.java        -- 잔액 조회 요청 (accountNo)
│   ├── BalanceResponse.java       -- 잔액 조회 응답 (accountNo, balance, ownerName)
│   ├── TransferRequest.java       -- 이체 요청 (fromAccount, toAccount, amount)
│   └── TransferResponse.java      -- 이체 응답 (success, message, transferId)
├── service/
│   └── BankService.java           -- 잔액조회, 출금, 정산이체 로직
├── controller/
│   └── BankController.java        -- REST API 3개
└── exception/
    ├── BankException.java         -- 잔액부족, 계좌미존재 예외
    └── GlobalExceptionHandler.java
```

---

### API 엔드포인트

| Method | Endpoint | 설명 | 호출자 |
|--------|----------|------|--------|
| POST | `/api/bank/balance` | 계좌 잔액 조회 | payment-service (체크카드 승인 시) |
| POST | `/api/bank/withdraw` | 출금 (사용자→카드사 이체) | payment-service (체크카드 승인 시) |
| POST | `/api/bank/transfer` | 정산 이체 (카드사→가맹점) | settlement-service (D+1 정산 시) |

### 요청/응답 예시

**잔액 조회**
```json
// POST /api/bank/balance
// Request
{ "accountNo": "1001-0001" }

// Response
{ "accountNo": "1001-0001", "balance": 1000000, "ownerName": "홍길동" }
```

**출금 (체크카드 승인)**
```json
// POST /api/bank/withdraw
// Request
{ "fromAccount": "1001-0001", "toAccount": "9000-0001", "amount": 50000 }

// Response (성공)
{ "success": true, "message": "출금 완료", "transferId": 1 }

// Response (잔액 부족)
{ "success": false, "code": "INSUFFICIENT_BALANCE", "message": "잔액이 부족합니다: 1001-0001" }
```

**정산 이체**
```json
// POST /api/bank/transfer
// Request
{ "fromAccount": "9000-0001", "toAccount": "2001-0001", "amount": 49000 }

// Response
{ "success": true, "message": "이체 완료", "transferId": 2 }
```

---

### 비즈니스 로직 설명

**BankService.checkBalance** — 계좌번호로 잔액 조회. 계좌 미존재 시 예외.

**BankService.withdraw** — 체크카드 승인용 출금.
- from 계좌 잔액 >= 요청 금액 확인
- from 잔액 차감, to 잔액 증가 (사용자→카드사)
- bank_transfer_log에 APPROVAL 타입으로 기록
- @Transactional로 원자성 보장

**BankService.transfer** — D+1 정산용 이체.
- from 잔액 차감, to 잔액 증가 (카드사→가맹점)
- bank_transfer_log에 SETTLE 타입으로 기록
- @Transactional로 원자성 보장

---

### 수정한 기존 파일

| 파일 | 변경 내용 |
|------|-----------|
| `banking-service/build.gradle` | Eureka Client 의존성 + Spring Cloud BOM 추가 |
| `banking-service/application.yaml` | Eureka 설정 추가 |
| `banking-service/BankingServiceApplication.java` | @EnableDiscoveryClient 추가 |
| `payment-service/build.gradle` | Eureka Client 의존성 + Spring Cloud BOM 추가 |
| `payment-service/application.yaml` | Eureka 설정 추가 |
| `payment-service/PaymentServiceApplication.java` | @EnableDiscoveryClient 추가 |
| `init-db/bank_db.sql` | 초기 테스트 데이터 INSERT 5건 추가 |

---

### 테스트 데이터 (bank_db.sql에 추가됨)

| 계좌번호 | 소유자 | 잔액 | 유형 |
|----------|--------|------|------|
| 1001-0001 | 홍길동 | 1,000,000 | USER |
| 1001-0002 | 김철수 | 500,000 | USER |
| 9000-0001 | 우리카드 | 0 | CARD_COMPANY |
| 2001-0001 | 스타벅스 | 0 | MERCHANT |
| 2001-0002 | CU편의점 | 0 | MERCHANT |

---

### 미구현 (추후 작업)

- **payment-service 체크카드 승인 로직** — 팀원과 충돌 방지를 위해 보류 중
- **취소/환불** — 추후 개발로 미룸

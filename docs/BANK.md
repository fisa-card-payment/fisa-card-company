## 🏦Banking-Service

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
``
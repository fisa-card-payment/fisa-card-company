## 💳Payment-Service

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

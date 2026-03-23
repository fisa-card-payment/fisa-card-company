## 🧵API Gateway (포트: 8080)

라우팅 규칙:

| 경로 | 대상 서비스 | 포트 |
|------|------------|------|
| `/api/payment/**` | payment-service | 8082 |
| `/payment/**` | payment-service | 8082 |
| `/api/settlement/**` | settlement-service | 8084 |

모든 요청에 `X-Trace-Id` 헤더 자동 생성:
```
[GATEWAY] TraceId: a1b2c3d4e5f6g7h8 | POST /api/payment/approve
```
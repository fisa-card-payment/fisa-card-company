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
VAN(8081) → API Gateway(8080) → Payment Service(8082)
                               → Settlement Service(8084)
```

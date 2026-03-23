# DB 구조 및 명세

### 1. ledger_source — `card_ledger` (결제 원장, 쓰기)


```sql
CREATE TABLE card_ledger (
    ledger_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    rrn              VARCHAR(12) UNIQUE NOT NULL,  -- 카드사 발급 거래번호
    stan             VARCHAR(6)  UNIQUE NOT NULL,  -- VAN 발급 추적번호
    card_number      VARCHAR(16) NOT NULL,
    merchant_id      VARCHAR(15) NOT NULL,
    amount           BIGINT NOT NULL,
    approval_code    VARCHAR(6),
    status           VARCHAR(10),                  -- APPROVED / REJECTED
    settlement_status VARCHAR(10),                 -- 정산 상태
    approved_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2. ledger_replica — `card_ledger` (정산 대사용, 읽기 전용)


`ledger_source`와 동일한 스키마. `settlement-service`가 원장 대사 시 읽기 전용으로 조회합니다.

### 3. shared_master — 공유 마스터 + 정산 스테이징


```sql
-- 카드 마스터
CREATE TABLE card_master (
    card_number     VARCHAR(16) PRIMARY KEY,
    user_id         VARCHAR(20),
    card_type       VARCHAR(10),       -- CREDIT, CHECK
    linked_account  VARCHAR(20),
    credit_limit    BIGINT DEFAULT 0,
    used_amount     BIGINT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'ACTIVE'
);

-- 가맹점 마스터
CREATE TABLE merchant_master (
    merchant_id     VARCHAR(32) PRIMARY KEY,
    merchant_name   VARCHAR(100),
    settle_account  VARCHAR(30),
    fee_rate        DECIMAL(5,4) DEFAULT 0.0200  -- 기본 2%
);
```

**테스트 카드 데이터 (shared_master.card_master):**


| 카드번호 | 타입 | 상태 | 비고 |
|---------|------|------|------|
| 1234567890120001 | CREDIT | ACTIVE | 한도 100만원, 미사용 |
| 1234567890120002 | CREDIT | ACTIVE | 한도 50만원, 45만원 사용 (잔여 5만원) |
| 1234567890120003 | CHECK | ACTIVE | 홍길동 계좌(1001-0001) 연결 |
| 1234567890120004 | CREDIT | INACTIVE | 정지 카드 (테스트용) |


```sql
-- VAN CSV 파일 수신 추적
CREATE TABLE van_settlement_file (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL,
    stored_path  VARCHAR(512),
    status       VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    row_count    INT NOT NULL DEFAULT 0,
    error_message VARCHAR(512),
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

-- CSV 행 단위 스테이징
CREATE TABLE van_settlement_staging (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id       BIGINT NOT NULL,
    line_no       INT NOT NULL,
    rrn           VARCHAR(64) NOT NULL,
    stan          VARCHAR(32) NOT NULL,
    card_number   VARCHAR(64) NOT NULL,
    amount        BIGINT NOT NULL,
    merchant_id   VARCHAR(32) NOT NULL,
    card_company  VARCHAR(64) NOT NULL,
    approval_code VARCHAR(32) NOT NULL,
    created_at_raw VARCHAR(64) NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_van_settlement_staging_file
        FOREIGN KEY (file_id) REFERENCES van_settlement_file (id) ON DELETE CASCADE
);
```


### 4. bank_system — 은행 계좌 및 이체 로그


```sql
CREATE TABLE bank_accounts (
    account_no    VARCHAR(20) PRIMARY KEY,
    owner_name    VARCHAR(50),
    balance       BIGINT DEFAULT 0,
    account_type  VARCHAR(50)  -- USER, CARD_COMPANY, MERCHANT
);

CREATE TABLE bank_transfer_log (
    transfer_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_acc       VARCHAR(20),
    to_acc         VARCHAR(20),
    amount         BIGINT,
    transfer_type  VARCHAR(20),  -- APPROVAL, SETTLE
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```


**테스트 초기 데이터:**

| 계좌번호 | 소유자 | 잔액 | 유형 |
|----------|--------|------|------|
| 1001-0001 | 홍길동 | 1,000,000 | USER |
| 1001-0002 | 김철수 | 500,000 | USER |
| 9000-0001 | 우리카드 | 1,000,000,000 | CARD_COMPANY |
| 9000-0002 | VAN사 | 0 | CARD_COMPANY |
| 2001-0001 | 스타벅스 | 0 | MERCHANT |
| 2001-0002 | CU편의점 | 0 | MERCHANT |

### 5. van_db — VAN 전용 DB (별도 컨테이너)


```sql
-- 결제 승인/거절 내역
CREATE TABLE van_transactions (
    van_tx_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    rrn           VARCHAR(20) UNIQUE NOT NULL,
    stan          VARCHAR(6) NOT NULL,
    card_number   VARCHAR(19) NOT NULL,
    amount        BIGINT NOT NULL,
    merchant_id   VARCHAR(15) NOT NULL,
    card_company  VARCHAR(10),
    response_code VARCHAR(2),
    approval_code VARCHAR(6),
    status        VARCHAR(10),               -- APPROVED / REJECTED
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 배치 실행 기록
CREATE TABLE acquisition_batches (
    batch_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_date   DATE NOT NULL,
    card_company VARCHAR(10),
    total_count  INT DEFAULT 0,
    total_amount BIGINT DEFAULT 0,
    file_name    VARCHAR(100),
    status       VARCHAR(10),               -- SENT / FAILED
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- BIN 테이블 (카드번호 앞 6자리 → 카드사 식별)
CREATE TABLE card_bins (
    bin_prefix    VARCHAR(6) PRIMARY KEY,
    company_name  VARCHAR(20),
    api_endpoint  VARCHAR(100)
);
```
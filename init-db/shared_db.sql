DROP TABLE IF EXISTS merchant_master;
DROP TABLE IF EXISTS card_master;

CREATE TABLE card_master (
                             card_number     VARCHAR(19) PRIMARY KEY,
                             user_id         VARCHAR(20),
                             card_type       VARCHAR(10),        -- CREDIT, CHECK
                             linked_account  VARCHAR(20),
                             credit_limit    BIGINT DEFAULT 0,
                             used_amount     BIGINT DEFAULT 0,
                             status          VARCHAR(10) DEFAULT 'ACTIVE'
);

CREATE TABLE merchant_master (
                                 merchant_id     VARCHAR(15) PRIMARY KEY,
                                 merchant_name   VARCHAR(100),
                                 settle_account  VARCHAR(20),
                                 fee_rate        DECIMAL(5,4) DEFAULT 0.0200   -- 2%
);

-- VAN 정산 CSV 수신 추적 + 스테이징 (settlement-service / API Gateway 수신)
DROP TABLE IF EXISTS van_settlement_staging;
DROP TABLE IF EXISTS van_settlement_file;

CREATE TABLE van_settlement_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    row_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(512),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE van_settlement_staging (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    rrn VARCHAR(64) NOT NULL,
    stan VARCHAR(32) NOT NULL,
    card_number VARCHAR(64) NOT NULL,
    amount BIGINT NOT NULL,
    merchant_id VARCHAR(32) NOT NULL,
    card_company VARCHAR(64) NOT NULL,
    approval_code VARCHAR(32) NOT NULL,
    created_at_raw VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_van_settlement_staging_file FOREIGN KEY (file_id) REFERENCES van_settlement_file (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_van_settlement_staging_file_id ON van_settlement_staging (file_id);

-- =============================================
-- 테스트 데이터 (bank_db의 계좌번호와 일치)
-- =============================================

-- 신용카드 (한도 100만원, 잔여 100만원) - 홍길동 계좌: 1001-0001
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234-5678-9012-0001', 'user_001', 'CREDIT', '1001-0001', 1000000, 0, 'ACTIVE');

-- 신용카드 (한도 50만원, 이미 45만원 사용 → 잔여 5만원) - 김철수 계좌: 1001-0002
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234-5678-9012-0002', 'user_002', 'CREDIT', '1001-0002', 500000, 450000, 'ACTIVE');

-- 체크카드 (CARD_TYPE_MISMATCH 테스트용) - 홍길동 계좌 연결
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234-5678-9012-0003', 'user_001', 'CHECK', '1001-0001', 0, 0, 'ACTIVE');

-- 정지된 신용카드 (CARD_INACTIVE 테스트용)
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234-5678-9012-0004', 'user_002', 'CREDIT', '1001-0002', 1000000, 0, 'INACTIVE');

-- 가맹점 (bank_db의 정산 계좌와 일치)
INSERT INTO merchant_master (merchant_id, merchant_name, settle_account, fee_rate)
VALUES ('MERCHANT_001', '스타벅스', '2001-0001', 0.0150);

INSERT INTO merchant_master (merchant_id, merchant_name, settle_account, fee_rate)
VALUES ('MERCHANT_002', 'CU편의점', '2001-0002', 0.0200);

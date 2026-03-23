DROP TABLE IF EXISTS merchant_master;
DROP TABLE IF EXISTS card_master;

CREATE TABLE card_master (
                             card_number     VARCHAR(16) PRIMARY KEY,
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

-- =============================================
-- 테스트 데이터 (bank_db의 계좌번호와 일치)
-- =============================================

-- 신용카드 (한도 100만원, 잔여 100만원) - 홍길동 계좌: 1001-0001
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234567890120001', 'user_001', 'CREDIT', '1001-0001', 1000000, 0, 'ACTIVE');

-- 신용카드 (한도 50만원, 이미 45만원 사용 → 잔여 5만원) - 김철수 계좌: 1001-0002
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234567890120002', 'user_002', 'CREDIT', '1001-0002', 500000, 450000, 'ACTIVE');

-- 체크카드 (CARD_TYPE_MISMATCH 테스트용) - 홍길동 계좌 연결
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234567890120003', 'user_001', 'CHECK', '1001-0001', 0, 0, 'ACTIVE');

-- 정지된 신용카드 (CARD_INACTIVE 테스트용)
INSERT INTO card_master (card_number, user_id, card_type, linked_account, credit_limit, used_amount, status)
VALUES ('1234567890120004', 'user_002', 'CREDIT', '1001-0002', 1000000, 0, 'INACTIVE');

-- 가맹점 (bank_db의 정산 계좌와 일치)
INSERT INTO merchant_master (merchant_id, merchant_name, settle_account, fee_rate)
VALUES ('MERCHANT_001', '스타벅스', '2001-0001', 0.0150);

INSERT INTO merchant_master (merchant_id, merchant_name, settle_account, fee_rate)
VALUES ('MERCHANT_002', 'CU편의점', '2001-0002', 0.0200);

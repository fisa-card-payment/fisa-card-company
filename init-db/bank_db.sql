DROP TABLE IF EXISTS bank_transfer_log;
DROP TABLE IF EXISTS bank_accounts;

CREATE TABLE bank_accounts (
                               account_no      VARCHAR(20) PRIMARY KEY,
                               owner_name      VARCHAR(50),
                               balance         BIGINT DEFAULT 0,
                               account_type    VARCHAR(50)  -- USER, CARD_COMPANY, MERCHANT
);

CREATE TABLE bank_transfer_log (
                                   transfer_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   from_acc        VARCHAR(20),
                                   to_acc          VARCHAR(20),
                                   amount          BIGINT,
                                   transfer_type   VARCHAR(20),  -- APPROVAL, SETTLE
                                   created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 초기 테스트 데이터
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('1001-0001', '홍길동', 1000000, 'USER');
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('1001-0002', '김철수', 500000, 'USER');
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('9000-0001', '우리카드', 1000000000, 'CARD_COMPANY');
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('9000-0002', 'VAN사', 0, 'CARD_COMPANY');
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('2001-0001', '스타벅스', 0, 'MERCHANT');
INSERT INTO bank_accounts (account_no, owner_name, balance, account_type) VALUES ('2001-0002', 'CU편의점', 0, 'MERCHANT');
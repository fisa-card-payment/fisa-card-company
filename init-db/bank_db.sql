DROP TABLE IF EXISTS bank_transfer_log;
DROP TABLE IF EXISTS bank_accounts;

CREATE TABLE bank_accounts (
                               account_no      VARCHAR(20) PRIMARY KEY,
                               owner_name      VARCHAR(50),
                               balance         BIGINT DEFAULT 0,
                               account_type    VARCHAR(10)  -- USER, CARD_COMPANY, MERCHANT
);

CREATE TABLE bank_transfer_log (
                                   transfer_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   from_acc        VARCHAR(20),
                                   to_acc          VARCHAR(20),
                                   amount          BIGINT,
                                   transfer_type   VARCHAR(20),  -- APPROVAL, SETTLE
                                   created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
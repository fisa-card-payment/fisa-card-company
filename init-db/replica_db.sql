DROP TABLE IF EXISTS card_ledger;

CREATE TABLE card_ledger (
                             ledger_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                             rrn             VARCHAR(12) UNIQUE NOT NULL,
                             stan            VARCHAR(6) UNIQUE NOT NULL,
                             card_number     VARCHAR(19) NOT NULL,
                             merchant_id     VARCHAR(15) NOT NULL,
                             amount          BIGINT NOT NULL,
                             approval_code   VARCHAR(6),
                             status          VARCHAR(10),
                             settlement_status    VARCHAR(10),
                             approved_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
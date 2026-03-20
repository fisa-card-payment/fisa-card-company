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
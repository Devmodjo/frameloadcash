-- Frameload Cash - schéma MySQL / MariaDB (données fictives uniquement).
-- Reconstruit toute la base : à relancer à chaque évolution du schéma en local.

DROP DATABASE IF EXISTS frameloadcash;
CREATE DATABASE frameloadcash CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE frameloadcash;

CREATE TABLE clients (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    client_number VARCHAR(20)  NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    address       VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clients_client_number UNIQUE (client_number),
    CONSTRAINT uk_clients_email UNIQUE (email),
    CONSTRAINT ck_clients_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB;

CREATE TABLE accounts (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)    NOT NULL,
    client_id      BIGINT         NOT NULL,
    account_type   VARCHAR(20)    NOT NULL,
    balance        DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)     NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     DATE           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT ck_accounts_balance CHECK (balance >= 0),
    CONSTRAINT ck_accounts_type CHECK (account_type IN ('CURRENT', 'SAVING')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
) ENGINE = InnoDB;

CREATE TABLE transactions (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    reference        VARCHAR(20)    NOT NULL,
    account_id       BIGINT         NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL,
    amount           DECIMAL(19, 2) NOT NULL,
    description      VARCHAR(255),
    status           VARCHAR(20)    NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_transactions_reference UNIQUE (reference),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('COMPLETED', 'FAILED')),
    INDEX idx_transactions_account_date (account_id, created_at)
) ENGINE = InnoDB;


CREATE TABLE transfers (
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    reference              VARCHAR(20)    NOT NULL,
    source_account_id      BIGINT         NOT NULL,
    destination_account_id BIGINT         NOT NULL,
    amount                 DECIMAL(19, 2) NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_transfers_reference UNIQUE (reference),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_destination FOREIGN KEY (destination_account_id) REFERENCES accounts (id),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT ck_transfers_status CHECK (status IN ('COMPLETED', 'FAILED'))
) ENGINE = InnoDB;

-- Colonne technique : rattache les deux écritures (TRANSFER_OUT, TRANSFER_IN) à leur virement.
ALTER TABLE transactions
    ADD COLUMN transfer_id BIGINT NULL,
    ADD CONSTRAINT fk_transactions_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id);
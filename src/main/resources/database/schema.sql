-- Frameload Cash - schéma MySQL / MariaDB (données fictives uniquement).
-- Ce script reconstruit toute la base : à relancer à chaque évolution du schéma en environnement local.

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
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clients_client_number UNIQUE (client_number),
    CONSTRAINT uk_clients_email UNIQUE (email),
    CONSTRAINT ck_clients_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB;
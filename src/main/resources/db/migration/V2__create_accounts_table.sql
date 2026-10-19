-- =====================================================================
-- Migration: V2__create_accounts_table.sql
-- Description: Provisioning the users and accounts table
-- =====================================================================

-- 1. Tạo bảng users trước vì accounts cần tham chiếu tới
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(30) NOT NULL DEFAULT 'USER'
    );

-- 2. Tạo bảng accounts
CREATE TABLE IF NOT EXISTS accounts (
                                        accountsid BIGSERIAL PRIMARY KEY,
                                        user_id BIGINT NOT NULL,
                                        account_number VARCHAR(32) NOT NULL UNIQUE,
    balance DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Referential integrity constraint linking to the users aggregate
    CONSTRAINT fk_accounts_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    );

-- =====================================================================
-- Performance indexes for lookup optimization and filtering
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
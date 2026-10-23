CREATE TABLE IF NOT EXISTS transfers (
                                         id BIGSERIAL PRIMARY KEY,
                                         reference_code VARCHAR(100) UNIQUE NOT NULL,
    source_account_id BIGINT NOT NULL,
    destination_account_id BIGINT NOT NULL,
    amount DECIMAL(18, 4) NOT NULL,
    fee DECIMAL(18, 4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transfer_source
    FOREIGN KEY (source_account_id)
    REFERENCES accounts(accountsid),

    CONSTRAINT fk_transfer_dest
    FOREIGN KEY (destination_account_id)
    REFERENCES accounts(accountsid),

    CONSTRAINT chk_transfer_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transfer_fee_non_negative CHECK (fee >= 0),
    CONSTRAINT chk_different_accounts CHECK (source_account_id <> destination_account_id)
    );
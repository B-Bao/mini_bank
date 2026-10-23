-- Thêm IF NOT EXISTS để Flyway không bị lỗi nếu bảng đã tồn tại
CREATE TABLE IF NOT EXISTS ledger_entries (
                                              id BIGSERIAL PRIMARY KEY,
                                              transaction_id UUID NOT NULL,
                                              account_id BIGINT NOT NULL,
                                              entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(19, 2) NOT NULL,
    balance_before DECIMAL(19, 2) NOT NULL,
    balance_after DECIMAL(19, 2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts(accountsid)
    );

-- Tạo Index an toàn
CREATE INDEX IF NOT EXISTS idx_ledger_account_id ON ledger_entries(account_id);
CREATE INDEX IF NOT EXISTS idx_ledger_transaction_id ON ledger_entries(transaction_id);
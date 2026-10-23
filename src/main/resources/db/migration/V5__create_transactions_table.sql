CREATE TABLE transactions (
                                   transaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   reference_number VARCHAR(50) UNIQUE NOT NULL,
                                   trace_number VARCHAR(50),

                                   sender_account_number VARCHAR(20) NOT NULL,
                                   sender_name VARCHAR(100) NOT NULL,
                                   receiver_account_number VARCHAR(20) NOT NULL,
                                   receiver_name VARCHAR(100) NOT NULL,
                                   beneficiary_bank_code VARCHAR(20) DEFAULT 'INTERNAL',

                                   amount DECIMAL(18, 2) NOT NULL,
                                   fee_amount DECIMAL(18, 2) DEFAULT 0.00,
                                   vat_amount DECIMAL(18, 2) DEFAULT 0.00,
                                   currency_code VARCHAR(3) DEFAULT 'VND',
                                   fee_payer VARCHAR(10) CHECK (fee_payer IN ('SENDER', 'RECEIVER')),

                                   transaction_type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAW, TRANSFER
                                   channel VARCHAR(20) NOT NULL,          -- MOBILE_APP, INTERNET_BANKING, API
                                   status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED

                                   description TEXT,
                                   device_id VARCHAR(100),
                                   ip_address VARCHAR(45),

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   value_date DATE NOT NULL,
                                   completed_at TIMESTAMP WITH TIME ZONE
);

-- Tạo các Index phục vụ tra cứu hiệu suất cao
CREATE INDEX idx_bank_tx_ref_number ON transactions(reference_number);
CREATE INDEX idx_bank_tx_sender_acc ON transactions(sender_account_number);
CREATE INDEX idx_bank_tx_receiver_acc ON transactions(receiver_account_number);
CREATE INDEX idx_bank_tx_status ON transactions(status);
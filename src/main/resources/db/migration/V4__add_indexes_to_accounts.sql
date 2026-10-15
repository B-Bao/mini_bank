-- Tạo Index trên cột foreign key user_id để tối ưu tốc độ truy vấn danh sách tài khoản của một user (GET /api/v1/accounts)
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);

-- Tạo Unique Index trên cột account_number để tối ưu tra cứu và đảm bảo tính duy nhất tuyệt đối
CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);
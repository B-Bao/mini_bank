-- Add the role used to authorize requests from JWT claims.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password VARCHAR(255);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

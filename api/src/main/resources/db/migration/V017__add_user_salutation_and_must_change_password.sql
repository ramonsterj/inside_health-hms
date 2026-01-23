-- Add salutation field (optional) and must_change_password flag to users table
ALTER TABLE users ADD COLUMN salutation VARCHAR(10);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

-- Comment for clarity
COMMENT ON COLUMN users.salutation IS 'User salutation/title (SR, SRA, SRTA, DR, DRA, MR, MRS, MISS)';
COMMENT ON COLUMN users.must_change_password IS 'Flag indicating user must change password on next login';

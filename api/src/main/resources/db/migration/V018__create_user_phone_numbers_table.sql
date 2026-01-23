-- Create user_phone_numbers table for storing multiple phone numbers per user
CREATE TABLE user_phone_numbers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    phone_number VARCHAR(20) NOT NULL,
    phone_type VARCHAR(20) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    -- BaseEntity audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_user_phone_numbers_user_id ON user_phone_numbers(user_id);
CREATE INDEX idx_user_phone_numbers_deleted_at ON user_phone_numbers(deleted_at);

-- Comments for clarity
COMMENT ON TABLE user_phone_numbers IS 'User phone numbers with type labels (MOBILE, PRACTICE, HOME, WORK, OTHER)';
COMMENT ON COLUMN user_phone_numbers.phone_type IS 'Phone type: MOBILE, PRACTICE, HOME, WORK, OTHER';
COMMENT ON COLUMN user_phone_numbers.is_primary IS 'Indicates if this is the primary contact number';

-- Add username column to users table
ALTER TABLE users ADD COLUMN username VARCHAR(50) UNIQUE;

-- Make username NOT NULL with a temporary default for existing rows
-- (generates username from email prefix for any existing users)
UPDATE users SET username = LOWER(SPLIT_PART(email, '@', 1)) WHERE username IS NULL;

-- Now make the column NOT NULL
ALTER TABLE users ALTER COLUMN username SET NOT NULL;

-- Add index for username lookups
CREATE INDEX idx_users_username ON users(username);

-- Insert default admin user
-- Password: admin123 (BCrypt hash)
INSERT INTO users (
    username,
    email,
    password_hash,
    first_name,
    last_name,
    role,
    status,
    email_verified,
    created_at,
    updated_at
) VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBrkHx9g8P4tJ.y8A0OBxqLPIYu', -- admin123
    'System',
    'Administrator',
    'ADMIN',
    'ACTIVE',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Add comment
COMMENT ON COLUMN users.username IS 'Unique username for login (alternative to email)';

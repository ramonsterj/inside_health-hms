-- Remove the old role enum column from users table
-- Role assignment is now handled via user_roles junction table
DROP INDEX IF EXISTS idx_users_role;
ALTER TABLE users DROP COLUMN role;

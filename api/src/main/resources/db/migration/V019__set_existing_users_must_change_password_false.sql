-- Set existing users to not require password change
-- New users created after this migration will have must_change_password = TRUE by default
UPDATE users SET must_change_password = FALSE WHERE deleted_at IS NULL;

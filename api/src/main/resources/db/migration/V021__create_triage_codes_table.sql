-- Create triage_codes table for admission triage classification
CREATE TABLE triage_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    color VARCHAR(7) NOT NULL,  -- Hex color #RRGGBB
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_triage_codes_deleted_at ON triage_codes(deleted_at);
CREATE INDEX idx_triage_codes_display_order ON triage_codes(display_order);

-- Seed default triage codes (standard emergency triage system)
INSERT INTO triage_codes (code, color, description, display_order, created_at, updated_at) VALUES
('A', '#FF0000', 'Critical - Immediate attention required', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('B', '#FFA500', 'Urgent - Requires prompt attention', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('C', '#FFFF00', 'Less Urgent - Can wait for care', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('D', '#00FF00', 'Non-Urgent - Minor issues', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('E', '#0000FF', 'Referral - Scheduled admission', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

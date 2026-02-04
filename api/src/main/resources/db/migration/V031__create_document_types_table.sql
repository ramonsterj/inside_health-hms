-- Create document_types table for configurable document categories
CREATE TABLE document_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    display_order INT NOT NULL DEFAULT 0,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_document_types_deleted_at ON document_types(deleted_at);
CREATE INDEX idx_document_types_code ON document_types(code);
CREATE INDEX idx_document_types_display_order ON document_types(display_order);

-- Seed psychiatric hospital document types
INSERT INTO document_types (code, name, description, display_order, created_at, updated_at) VALUES
('CONSENT_ADMISSION', 'Admission Consent', 'General admission consent form', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_ISOLATION', 'Isolation Consent', 'Consent for patient isolation/seclusion', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_RESTRAINT', 'Restraint Consent', 'Consent for physical restraints (immobilization)', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('CONSENT_SEDATION', 'Sedation Consent', 'Consent for sedation/medication administration', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('INVENTORY_LIST', 'Inventory List', 'Written inventory of patient belongings', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('INVENTORY_PHOTO', 'Inventory Photos', 'Photos of patient belongings', 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OTHER', 'Other Document', 'Other admission-related documents', 99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- V101: Pharmacy & Inventory Evolution — Phase 1b
-- Creates the MedicationDetails satellite (1:1, kind=DRUG).

CREATE TABLE medication_details (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    generic_name VARCHAR(150) NOT NULL,
    commercial_name VARCHAR(150) NULL,
    strength VARCHAR(50) NULL,
    dosage_form VARCHAR(30) NOT NULL,
    route VARCHAR(20) NULL,
    controlled BOOLEAN NOT NULL DEFAULT FALSE,
    atc_code VARCHAR(10) NULL,
    section VARCHAR(30) NOT NULL,
    review_status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    review_notes VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_medication_details_item_id
    ON medication_details(item_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_medication_details_deleted_at ON medication_details(deleted_at);
CREATE INDEX idx_medication_details_section ON medication_details(section);
CREATE INDEX idx_medication_details_review_status ON medication_details(review_status);

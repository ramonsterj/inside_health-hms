-- Clinical History (Historia Clinica) - One record per admission
-- Contains comprehensive medical-psychiatric history fields as rich text

CREATE TABLE clinical_histories (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL UNIQUE,

    -- Medical-Psychiatric History Fields (all TEXT for rich content)
    reason_for_admission TEXT,
    history_of_present_illness TEXT,
    psychiatric_history TEXT,
    medical_history TEXT,
    family_history TEXT,
    personal_history TEXT,
    substance_use_history TEXT,
    legal_history TEXT,
    social_history TEXT,
    developmental_history TEXT,
    educational_occupational_history TEXT,
    sexual_history TEXT,
    religious_spiritual_history TEXT,
    mental_status_exam TEXT,
    physical_exam TEXT,
    diagnostic_impression TEXT,
    treatment_plan TEXT,
    risk_assessment TEXT,
    prognosis TEXT,
    informed_consent_notes TEXT,
    additional_notes TEXT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_clinical_history_admission FOREIGN KEY (admission_id) REFERENCES admissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_clinical_history_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_clinical_history_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE INDEX idx_clinical_histories_deleted_at ON clinical_histories(deleted_at);
CREATE INDEX idx_clinical_histories_admission_id ON clinical_histories(admission_id);

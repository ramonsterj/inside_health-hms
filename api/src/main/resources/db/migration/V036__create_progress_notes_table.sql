-- Progress Notes (Evoluciones) - Multiple SOAP-like entries per admission
-- Each note captures a clinical encounter with subjective/objective/analysis/plan

CREATE TABLE progress_notes (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,

    -- SOAP structure (all required)
    subjective_data TEXT NOT NULL,
    objective_data TEXT NOT NULL,
    analysis TEXT NOT NULL,
    action_plans TEXT NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_progress_note_admission FOREIGN KEY (admission_id) REFERENCES admissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_note_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_progress_note_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE INDEX idx_progress_notes_deleted_at ON progress_notes(deleted_at);
CREATE INDEX idx_progress_notes_admission_id ON progress_notes(admission_id);
CREATE INDEX idx_progress_notes_created_at ON progress_notes(created_at);

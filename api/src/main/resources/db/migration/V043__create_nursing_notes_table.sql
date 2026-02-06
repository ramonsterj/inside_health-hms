-- Create nursing_notes table
CREATE TABLE nursing_notes (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_nursing_notes_admission_id FOREIGN KEY (admission_id) REFERENCES admissions(id),
    CONSTRAINT fk_nursing_notes_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_nursing_notes_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE INDEX idx_nursing_notes_admission_id ON nursing_notes(admission_id);
CREATE INDEX idx_nursing_notes_created_at ON nursing_notes(created_at);
CREATE INDEX idx_nursing_notes_deleted_at ON nursing_notes(deleted_at);

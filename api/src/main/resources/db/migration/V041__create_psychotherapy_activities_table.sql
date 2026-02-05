-- Create psychotherapy activities table
CREATE TABLE psychotherapy_activities (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admissions(id),
    category_id BIGINT NOT NULL REFERENCES psychotherapy_categories(id),
    description VARCHAR(2000) NOT NULL,
    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_psychotherapy_activities_deleted_at ON psychotherapy_activities(deleted_at);
CREATE INDEX idx_psychotherapy_activities_admission_id ON psychotherapy_activities(admission_id);
CREATE INDEX idx_psychotherapy_activities_category_id ON psychotherapy_activities(category_id);
CREATE INDEX idx_psychotherapy_activities_created_at ON psychotherapy_activities(created_at);

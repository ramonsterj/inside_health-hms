-- V099: Admission PDF Export
--
-- Adds the `admission:export-pdf` permission used by the synchronous
-- admission-export endpoint, grants it to ADMIN and ADMINISTRATIVE_STAFF, and
-- extends `audit_logs` with nullable `status` and `details` columns so the
-- export service can record SUCCESS / FAILED attempts with structured metadata
-- (no PHI).
--
-- See `docs/features/admission-export.md` v1.2.

INSERT INTO permissions (code, name, description, resource, action, created_at, updated_at) VALUES
('admission:export-pdf', 'Export Admission PDF', 'Generate and download the full admission PDF', 'admission', 'export-pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'ADMINISTRATIVE_STAFF')
  AND p.code = 'admission:export-pdf'
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

ALTER TABLE audit_logs ADD COLUMN status VARCHAR(20) NULL;
ALTER TABLE audit_logs ADD COLUMN details JSONB NULL;

CREATE INDEX idx_audit_logs_status ON audit_logs(status);

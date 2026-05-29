-- ============================================================================
-- V117: Add AUXILIARY_NURSE role with a restricted nursing permission set
-- ============================================================================
-- Customer feedback (2026-05-26) splits the single operational NURSE role into
-- two real hospital roles: the graduate nurse (full clinical scope — keeps the
-- existing NURSE role unchanged) and the auxiliary nurse, who may only record
-- vital signs and write nursing notes. The auxiliary may NOT administer
-- medications, mark medical orders in progress, upload result documents, or
-- author progress notes.
--
-- Unlike V114 (which cloned the full DOCTOR grant onto RESIDENT_DOCTOR), the
-- auxiliary set is an explicit SUBSET — see the grant list below and the
-- deliberate exclusions noted at the end. Service-layer guards in
-- MedicationAdministrationService / MedicalOrderService / MedicalOrderDocumentService
-- enforce the three denied actions even if a custom role somehow grants the
-- underlying permission. Spec: docs/features/nursing-roles-split.md.
-- ============================================================================

INSERT INTO roles (code, name, description, is_system, created_at, updated_at)
VALUES (
    'AUXILIARY_NURSE',
    'Auxiliary Nurse',
    'Enfermero auxiliar — notes and vital signs only',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT
    (SELECT id FROM roles WHERE code = 'AUXILIARY_NURSE'),
    p.id,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM permissions p
WHERE p.code IN (
    'nursing-note:read', 'nursing-note:create',
    'vital-sign:read', 'vital-sign:create',
    'medication-administration:read',
    'medical-order:read',
    'progress-note:read',
    'clinical-history:read',
    'patient:read',
    'admission:read',
    'room:occupancy-view'
)
  AND p.deleted_at IS NULL
ON CONFLICT DO NOTHING;

-- Deliberate exclusions (enforced here by omission + service-layer guards):
--   medication-administration:create  — auxiliary cannot dispense.
--   medical-order:mark-in-progress    — auxiliary cannot execute orders.
--   medical-order:upload-document     — auxiliary cannot attach results.
--   progress-note:create              — auxiliary cannot author evoluciones.
--   admission:update                  — auxiliary cannot discharge patients or edit
--                                        admission metadata / consulting physicians. The
--                                        single admission:update permission gates the
--                                        discharge + edit + consulting-physician endpoints,
--                                        so granting it would breach the notes/vitals-only
--                                        scope. admission:read alone gives the kardex/patient
--                                        visibility the auxiliary needs.

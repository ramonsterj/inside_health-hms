-- ============================================================================
-- V116: Grant PSYCHOLOGIST the medical-order permissions needed to execute and
-- record results on psychometric tests (PRUEBAS_PSICOMETRICAS).
-- ============================================================================
-- Psychologists administer psychometric tests and produce the result report.
-- They need to:
--   * see the cross-admission orders dashboard,
--   * mark a psychometric order as EN_PROCESO,
--   * upload the result document.
--
-- Service-layer enforcement guarantees these capabilities are scoped to the
-- PRUEBAS_PSICOMETRICAS category only — psychologists must never act on
-- medications, labs, referrals, or directive orders. The permission grants
-- below are purely the API-surface gate; the category guard lives in
-- MedicalOrderService / MedicalOrderDocumentService.
-- ============================================================================

INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'PSYCHOLOGIST'
  AND p.code IN ('medical-order:read',
                 'medical-order:mark-in-progress',
                 'medical-order:upload-document')
  AND r.deleted_at IS NULL AND p.deleted_at IS NULL
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- V092: Migrate medical_orders.status to the category-driven state machine.
--
-- Three category shapes:
--   * Directive (no flow):              ACTIVA → DESCONTINUADO
--   * Authorize-only (medications):     SOLICITADO → AUTORIZADO → DESCONTINUADO
--                                                  → NO_AUTORIZADO
--   * Authorize + execute + results:    SOLICITADO → AUTORIZADO → EN_PROCESO → RESULTADOS_RECIBIDOS
--     (labs, referrals, psychometric)              → NO_AUTORIZADO  ↘ DESCONTINUADO (only before EN_PROCESO)
--
-- Discontinue is allowed from ACTIVA, SOLICITADO, AUTORIZADO. Once an order moves to
-- EN_PROCESO (sample taken / patient referred / test administered) it is at the lab
-- and can no longer be cancelled.

-- Workflow audit columns
ALTER TABLE medical_orders
    ADD COLUMN authorized_at         TIMESTAMP,
    ADD COLUMN authorized_by         BIGINT,
    ADD COLUMN in_progress_at        TIMESTAMP,
    ADD COLUMN in_progress_by        BIGINT,
    ADD COLUMN results_received_at   TIMESTAMP,
    ADD COLUMN results_received_by   BIGINT,
    ADD COLUMN rejection_reason      TEXT,
    ADD COLUMN emergency_authorized  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN emergency_reason      VARCHAR(40),
    ADD COLUMN emergency_reason_note TEXT,
    ADD COLUMN emergency_at          TIMESTAMP,
    ADD COLUMN emergency_by          BIGINT;

ALTER TABLE medical_orders
    ADD CONSTRAINT fk_medical_order_authorized_by       FOREIGN KEY (authorized_by)       REFERENCES users(id),
    ADD CONSTRAINT fk_medical_order_in_progress_by      FOREIGN KEY (in_progress_by)      REFERENCES users(id),
    ADD CONSTRAINT fk_medical_order_results_received_by FOREIGN KEY (results_received_by) REFERENCES users(id),
    ADD CONSTRAINT fk_medical_order_emergency_by        FOREIGN KEY (emergency_by)        REFERENCES users(id);

-- Drop the old status check constraint (if any) and widen the column to fit the
-- longest new value (RESULTADOS_RECIBIDOS = 20 chars; keep some headroom).
ALTER TABLE medical_orders DROP CONSTRAINT IF EXISTS chk_medical_order_status;
ALTER TABLE medical_orders ALTER COLUMN status TYPE VARCHAR(30);

-- Backfill: rename pre-existing 'ACTIVE' rows to category-appropriate states.
-- Directive categories (no auth needed) -> ACTIVA.
-- Auth-required categories -> AUTORIZADO (closest semantic — these orders were already in effect).
UPDATE medical_orders
   SET status = 'ACTIVA'
 WHERE status = 'ACTIVE'
   AND category IN ('ORDENES_MEDICAS','ACTIVIDAD_FISICA','CUIDADOS_ESPECIALES',
                    'DIETA','RESTRICCIONES_MOVILIDAD','PERMISOS_VISITA','OTRAS');
UPDATE medical_orders SET status = 'AUTORIZADO' WHERE status = 'ACTIVE';

-- Rename DISCONTINUED -> DESCONTINUADO for Spanish consistency with the rest of the enum.
UPDATE medical_orders SET status = 'DESCONTINUADO' WHERE status = 'DISCONTINUED';

-- Recreate the check constraint with the new allowed values.
ALTER TABLE medical_orders ADD CONSTRAINT chk_medical_order_status CHECK (status IN (
    'ACTIVA',
    'SOLICITADO',
    'NO_AUTORIZADO',
    'AUTORIZADO',
    'EN_PROCESO',
    'RESULTADOS_RECIBIDOS',
    'DESCONTINUADO'
));

-- Constrain emergency_reason to its enum domain.
ALTER TABLE medical_orders ADD CONSTRAINT chk_medical_order_emergency_reason CHECK (
    emergency_reason IS NULL OR emergency_reason IN (
        'PATIENT_IN_CRISIS','AFTER_HOURS_NO_ADMIN','FAMILY_UNREACHABLE','OTHER'
    )
);

-- The default for new rows is set application-side (MedicalOrderCategory.initialStatus())
-- because it's category-dependent.
ALTER TABLE medical_orders ALTER COLUMN status DROP DEFAULT;

-- Composite index to power the cross-admission orders-by-state dashboard.
CREATE INDEX idx_medical_orders_status_created_at ON medical_orders(status, created_at DESC);

-- Partial index for the emergency-authorize review report (small, hot).
CREATE INDEX idx_medical_orders_emergency ON medical_orders(emergency_authorized)
    WHERE emergency_authorized = TRUE;

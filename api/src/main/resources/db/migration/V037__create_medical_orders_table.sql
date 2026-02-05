-- Medical Orders (Ordenes Medicas) - Multiple orders per admission grouped by category
-- Supports medication orders and general medical orders with discontinue tracking

CREATE TABLE medical_orders (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL,

    -- Category (required) - uses VARCHAR for simplicity over PostgreSQL ENUM
    category VARCHAR(50) NOT NULL,

    -- Timing
    start_date DATE NOT NULL,
    end_date DATE,

    -- Medication-specific fields (optional, used for MEDICAMENTOS category)
    medication VARCHAR(255),
    dosage VARCHAR(100),
    route VARCHAR(50),
    frequency VARCHAR(100),
    schedule VARCHAR(100),

    -- General fields
    observations TEXT,

    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    discontinued_at TIMESTAMP,
    discontinued_by BIGINT,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP,

    CONSTRAINT fk_medical_order_admission FOREIGN KEY (admission_id) REFERENCES admissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_order_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_medical_order_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_medical_order_discontinued_by FOREIGN KEY (discontinued_by) REFERENCES users(id),
    CONSTRAINT chk_medical_order_category CHECK (category IN (
        'ORDENES_MEDICAS', 'MEDICAMENTOS', 'LABORATORIOS', 'REFERENCIAS_MEDICAS',
        'PRUEBAS_PSICOMETRICAS', 'ACTIVIDAD_FISICA', 'CUIDADOS_ESPECIALES',
        'DIETA', 'RESTRICCIONES_MOVILIDAD', 'PERMISOS_VISITA', 'OTRAS'
    )),
    CONSTRAINT chk_medical_order_route CHECK (route IS NULL OR route IN (
        'ORAL', 'IV', 'IM', 'SC', 'TOPICAL', 'INHALATION', 'RECTAL', 'SUBLINGUAL', 'OTHER'
    )),
    CONSTRAINT chk_medical_order_status CHECK (status IN ('ACTIVE', 'DISCONTINUED'))
);

CREATE INDEX idx_medical_orders_deleted_at ON medical_orders(deleted_at);
CREATE INDEX idx_medical_orders_admission_id ON medical_orders(admission_id);
CREATE INDEX idx_medical_orders_category ON medical_orders(category);
CREATE INDEX idx_medical_orders_status ON medical_orders(status);
CREATE INDEX idx_medical_orders_start_date ON medical_orders(start_date);

-- ============================================================================
-- V124: Add provider-aware, multi-line model to LABORATORIOS medical orders.
-- medical_orders gains a nullable lab_provider_id; medical_order_lab_tests holds
-- the snapshotted line items. See docs/features/laboratory-orders-with-providers.md.
-- ============================================================================

ALTER TABLE medical_orders ADD COLUMN lab_provider_id BIGINT REFERENCES lab_providers(id);
CREATE INDEX idx_medical_orders_lab_provider ON medical_orders(lab_provider_id);

CREATE TABLE medical_order_lab_tests (
    id BIGSERIAL PRIMARY KEY,
    medical_order_id BIGINT NOT NULL REFERENCES medical_orders(id),
    lab_provider_test_id BIGINT NOT NULL REFERENCES lab_provider_tests(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    display_name VARCHAR(200) NOT NULL,                          -- snapshot
    cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),               -- snapshot
    sales_price NUMERIC(12,2) NOT NULL CHECK (sales_price > 0),  -- snapshot
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_mo_lab_tests_deleted_at ON medical_order_lab_tests(deleted_at);
CREATE INDEX idx_mo_lab_tests_order ON medical_order_lab_tests(medical_order_id);
CREATE INDEX idx_mo_lab_tests_provider_test ON medical_order_lab_tests(lab_provider_test_id);
CREATE INDEX idx_mo_lab_tests_test ON medical_order_lab_tests(lab_test_id);

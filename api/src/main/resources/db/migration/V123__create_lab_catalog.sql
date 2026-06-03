-- ============================================================================
-- V123: Lab catalog tables — providers, canonical tests, per-provider priced
-- tests, panels, and panel items. See docs/features/laboratory-orders-with-providers.md.
-- ============================================================================

CREATE TABLE lab_providers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_providers_deleted_at ON lab_providers(deleted_at);
CREATE UNIQUE INDEX uq_lab_providers_name_active
  ON lab_providers(LOWER(name)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_lab_providers_code_active
  ON lab_providers(LOWER(code)) WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE lab_tests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_tests_deleted_at ON lab_tests(deleted_at);
CREATE UNIQUE INDEX uq_lab_tests_name_active
  ON lab_tests(LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE lab_provider_tests (
    id BIGSERIAL PRIMARY KEY,
    provider_id BIGINT NOT NULL REFERENCES lab_providers(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    display_name VARCHAR(200) NOT NULL,
    cost NUMERIC(12,2) NOT NULL CHECK (cost >= 0),
    sales_price NUMERIC(12,2) NOT NULL CHECK (sales_price > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_provider_tests_deleted_at ON lab_provider_tests(deleted_at);
CREATE INDEX idx_lab_provider_tests_provider ON lab_provider_tests(provider_id);
CREATE INDEX idx_lab_provider_tests_test ON lab_provider_tests(lab_test_id);
-- one non-deleted offering per (provider, canonical test)
CREATE UNIQUE INDEX uq_lab_provider_test_active
  ON lab_provider_tests(provider_id, lab_test_id) WHERE deleted_at IS NULL;

CREATE TABLE lab_panels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_panels_deleted_at ON lab_panels(deleted_at);
CREATE UNIQUE INDEX uq_lab_panels_name_active
  ON lab_panels(LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE lab_panel_items (
    id BIGSERIAL PRIMARY KEY,
    panel_id BIGINT NOT NULL REFERENCES lab_panels(id),
    lab_test_id BIGINT NOT NULL REFERENCES lab_tests(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP
);
CREATE INDEX idx_lab_panel_items_deleted_at ON lab_panel_items(deleted_at);
CREATE INDEX idx_lab_panel_items_panel ON lab_panel_items(panel_id);
CREATE INDEX idx_lab_panel_items_test ON lab_panel_items(lab_test_id);
CREATE UNIQUE INDEX uq_lab_panel_items_active
  ON lab_panel_items(panel_id, lab_test_id) WHERE deleted_at IS NULL;

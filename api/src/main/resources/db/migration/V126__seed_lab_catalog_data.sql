-- ============================================================================
-- V126: Seed the lab catalog from the LABORATORIOS PDF (providers, canonical
-- tests, per-provider offerings, panels).
--
-- ⚠️ PRICES ARE INVENTED DEMO DATA. The source PDF has no cost/sales-price
-- values. These figures only make the full demo/QA billing flow work now;
-- finance MUST replace them via the admin UI (or a follow-up migration) before
-- production billing is trusted. Invariant kept: cost < sales_price, sales_price > 0.
--
-- Idempotent (WHERE NOT EXISTS), mirrors the V111 prod reference-data approach.
-- See docs/features/laboratory-orders-with-providers.md.
-- ============================================================================

-- ---------- Providers ----------
INSERT INTO lab_providers (name, code, active, created_at, updated_at)
SELECT v.name, v.code, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('CLONY', 'CLONY'),
    ('HOSPITAL HERRERA LLERANDI', 'HERRERA')
) AS v(name, code)
WHERE NOT EXISTS (
    SELECT 1 FROM lab_providers p WHERE LOWER(p.name) = LOWER(v.name) AND p.deleted_at IS NULL
);

-- ---------- Canonical tests ----------
INSERT INTO lab_tests (name, active, created_at, updated_at)
SELECT v.name, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Hematología completa'),
    ('BUN'),
    ('Creatinina'),
    ('TGO'),
    ('TGP'),
    ('Sodio (Na)'),
    ('Potasio (K)'),
    ('Perfil de lípidos'),
    ('T3'),
    ('T4'),
    ('TSH'),
    ('Vitamina D'),
    ('Vitamina B12'),
    ('Panel de drogas en orina'),
    ('Panel de drogas en sangre (Ketamina y Fentanilo)'),
    ('Orina simple'),
    ('Glucosa al azar'),
    ('Prueba de embarazo'),
    ('Niveles de litio (litemia)'),
    ('Niveles de ácido valproico'),
    ('Niveles de amonio')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM lab_tests t WHERE LOWER(t.name) = LOWER(v.name) AND t.deleted_at IS NULL
);

-- ---------- Provider tests (per-provider name + invented prices) ----------
INSERT INTO lab_provider_tests (provider_id, lab_test_id, display_name, cost, sales_price, active, created_at, updated_at)
SELECT pr.id, t.id, v.display_name, v.cost, v.sales_price, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    -- CLONY offers every canonical test. Hematología completa demonstrates per-provider naming.
    ('CLONY', 'Hematología completa', 'Hemograma completo', 40, 75),
    ('CLONY', 'BUN', 'BUN', 30, 55),
    ('CLONY', 'Creatinina', 'Creatinina', 30, 55),
    ('CLONY', 'TGO', 'TGO', 35, 65),
    ('CLONY', 'TGP', 'TGP', 35, 65),
    ('CLONY', 'Sodio (Na)', 'Sodio (Na)', 28, 50),
    ('CLONY', 'Potasio (K)', 'Potasio (K)', 28, 50),
    ('CLONY', 'Perfil de lípidos', 'Perfil de lípidos', 70, 130),
    ('CLONY', 'T3', 'T3', 120, 220),
    ('CLONY', 'T4', 'T4', 120, 220),
    ('CLONY', 'TSH', 'TSH', 120, 220),
    ('CLONY', 'Vitamina D', 'Vitamina D', 150, 280),
    ('CLONY', 'Vitamina B12', 'Vitamina B12', 110, 200),
    ('CLONY', 'Panel de drogas en orina', 'Panel de drogas en orina', 180, 330),
    ('CLONY', 'Panel de drogas en sangre (Ketamina y Fentanilo)', 'Panel de drogas en sangre (Ketamina y Fentanilo)', 350, 620),
    ('CLONY', 'Orina simple', 'Orina simple', 25, 45),
    ('CLONY', 'Glucosa al azar', 'Glucosa al azar', 20, 40),
    ('CLONY', 'Prueba de embarazo', 'Prueba de embarazo', 35, 65),
    ('CLONY', 'Niveles de litio (litemia)', 'Niveles de litio (litemia)', 90, 165),
    ('CLONY', 'Niveles de ácido valproico', 'Niveles de ácido valproico', 100, 185),
    ('CLONY', 'Niveles de amonio', 'Niveles de amonio', 110, 200),
    -- HOSPITAL HERRERA LLERANDI offers everything EXCEPT Panel de drogas en sangre.
    ('HOSPITAL HERRERA LLERANDI', 'Hematología completa', 'Hematología completa', 45, 85),
    ('HOSPITAL HERRERA LLERANDI', 'BUN', 'BUN', 32, 60),
    ('HOSPITAL HERRERA LLERANDI', 'Creatinina', 'Creatinina', 32, 60),
    ('HOSPITAL HERRERA LLERANDI', 'TGO', 'TGO', 38, 70),
    ('HOSPITAL HERRERA LLERANDI', 'TGP', 'TGP', 38, 70),
    ('HOSPITAL HERRERA LLERANDI', 'Sodio (Na)', 'Sodio (Na)', 30, 55),
    ('HOSPITAL HERRERA LLERANDI', 'Potasio (K)', 'Potasio (K)', 30, 55),
    ('HOSPITAL HERRERA LLERANDI', 'Perfil de lípidos', 'Perfil de lípidos', 75, 140),
    ('HOSPITAL HERRERA LLERANDI', 'T3', 'T3', 130, 240),
    ('HOSPITAL HERRERA LLERANDI', 'T4', 'T4', 130, 240),
    ('HOSPITAL HERRERA LLERANDI', 'TSH', 'TSH', 130, 240),
    ('HOSPITAL HERRERA LLERANDI', 'Vitamina D', 'Vitamina D', 160, 300),
    ('HOSPITAL HERRERA LLERANDI', 'Vitamina B12', 'Vitamina B12', 120, 220),
    ('HOSPITAL HERRERA LLERANDI', 'Panel de drogas en orina', 'Panel de drogas en orina', 190, 350),
    ('HOSPITAL HERRERA LLERANDI', 'Orina simple', 'Orina simple', 27, 50),
    ('HOSPITAL HERRERA LLERANDI', 'Glucosa al azar', 'Glucosa al azar', 22, 42),
    ('HOSPITAL HERRERA LLERANDI', 'Prueba de embarazo', 'Prueba de embarazo', 38, 70),
    ('HOSPITAL HERRERA LLERANDI', 'Niveles de litio (litemia)', 'Niveles de litio (litemia)', 95, 175),
    ('HOSPITAL HERRERA LLERANDI', 'Niveles de ácido valproico', 'Niveles de ácido valproico', 105, 195),
    ('HOSPITAL HERRERA LLERANDI', 'Niveles de amonio', 'Niveles de amonio', 120, 220)
) AS v(provider_name, test_name, display_name, cost, sales_price)
JOIN lab_providers pr ON LOWER(pr.name) = LOWER(v.provider_name) AND pr.deleted_at IS NULL
JOIN lab_tests t ON LOWER(t.name) = LOWER(v.test_name) AND t.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM lab_provider_tests x
    WHERE x.provider_id = pr.id AND x.lab_test_id = t.id AND x.deleted_at IS NULL
);

-- ---------- Panels ----------
INSERT INTO lab_panels (name, active, created_at, updated_at)
SELECT v.name, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Laboratorios de ingreso'),
    ('Laboratorios de ingreso — mujeres en edad fértil'),
    ('Laboratorios control')
) AS v(name)
WHERE NOT EXISTS (
    SELECT 1 FROM lab_panels p WHERE LOWER(p.name) = LOWER(v.name) AND p.deleted_at IS NULL
);

-- ---------- Panel items ----------
INSERT INTO lab_panel_items (panel_id, lab_test_id, created_at, updated_at)
SELECT pa.id, t.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    -- Laboratorios de ingreso (includes Panel de drogas en sangre — the test Herrera lacks)
    ('Laboratorios de ingreso', 'Hematología completa'),
    ('Laboratorios de ingreso', 'BUN'),
    ('Laboratorios de ingreso', 'Creatinina'),
    ('Laboratorios de ingreso', 'TGO'),
    ('Laboratorios de ingreso', 'TGP'),
    ('Laboratorios de ingreso', 'Sodio (Na)'),
    ('Laboratorios de ingreso', 'Potasio (K)'),
    ('Laboratorios de ingreso', 'Perfil de lípidos'),
    ('Laboratorios de ingreso', 'T3'),
    ('Laboratorios de ingreso', 'T4'),
    ('Laboratorios de ingreso', 'TSH'),
    ('Laboratorios de ingreso', 'Vitamina D'),
    ('Laboratorios de ingreso', 'Vitamina B12'),
    ('Laboratorios de ingreso', 'Panel de drogas en orina'),
    ('Laboratorios de ingreso', 'Panel de drogas en sangre (Ketamina y Fentanilo)'),
    ('Laboratorios de ingreso', 'Orina simple'),
    ('Laboratorios de ingreso', 'Glucosa al azar'),
    -- Mujeres en edad fértil = ingreso + Prueba de embarazo
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Hematología completa'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'BUN'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Creatinina'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'TGO'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'TGP'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Sodio (Na)'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Potasio (K)'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Perfil de lípidos'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'T3'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'T4'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'TSH'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Vitamina D'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Vitamina B12'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Panel de drogas en orina'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Panel de drogas en sangre (Ketamina y Fentanilo)'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Orina simple'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Glucosa al azar'),
    ('Laboratorios de ingreso — mujeres en edad fértil', 'Prueba de embarazo'),
    -- Laboratorios control
    ('Laboratorios control', 'Niveles de litio (litemia)'),
    ('Laboratorios control', 'Niveles de ácido valproico'),
    ('Laboratorios control', 'Niveles de amonio')
) AS v(panel_name, test_name)
JOIN lab_panels pa ON LOWER(pa.name) = LOWER(v.panel_name) AND pa.deleted_at IS NULL
JOIN lab_tests t ON LOWER(t.name) = LOWER(v.test_name) AND t.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM lab_panel_items x
    WHERE x.panel_id = pa.id AND x.lab_test_id = t.id AND x.deleted_at IS NULL
);

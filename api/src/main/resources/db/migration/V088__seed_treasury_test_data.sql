-- V088: Seed treasury test data for dashboard/report visualization
-- This adds realistic hospital treasury data spanning Jan-Mar 2026

-- ============================================================
-- 1. Bank Accounts (id=1 is Caja Chica from V074)
-- ============================================================
INSERT INTO bank_accounts (name, bank_name, account_number, account_type, currency, opening_balance, is_petty_cash, notes)
VALUES
    ('Cuenta Operativa', 'Banco Industrial', '010-123456-7', 'CHECKING', 'GTQ', 150000.00, FALSE, 'Cuenta principal para operaciones'),
    ('Cuenta de Ahorro', 'Banco G&T Continental', '020-987654-3', 'SAVINGS', 'GTQ', 75000.00, FALSE, 'Reserva de emergencia');

-- Update Caja Chica to have a small opening balance
UPDATE bank_accounts SET opening_balance = 5000.00 WHERE id = 1;

-- ============================================================
-- 2. Treasury Employees
-- ============================================================
INSERT INTO treasury_employees (full_name, employee_type, tax_id, position, base_salary, hire_date, active)
VALUES
    ('María López García', 'PAYROLL', '1234567-8', 'Enfermera Jefe', 8500.00, '2024-03-15', TRUE),
    ('Carlos Rodríguez Pérez', 'PAYROLL', '2345678-9', 'Administrador', 12000.00, '2023-08-01', TRUE),
    ('Ana Martínez Flores', 'PAYROLL', '3456789-0', 'Enfermera', 6500.00, '2025-01-10', TRUE),
    ('Pedro Hernández Díaz', 'PAYROLL', '4567890-1', 'Mantenimiento', 4500.00, '2024-06-01', TRUE),
    ('Lucía Ramírez Torres', 'PAYROLL', '5678901-2', 'Recepcionista', 4200.00, '2025-06-15', TRUE);

INSERT INTO treasury_employees (full_name, employee_type, tax_id, position, contracted_rate, hire_date, active)
VALUES
    ('Roberto Sánchez Morales', 'CONTRACTOR', '6789012-3', 'Servicio de Limpieza', 3500.00, '2025-02-01', TRUE),
    ('Servicios de Seguridad S.A.', 'CONTRACTOR', '7890123-4', 'Vigilancia', 8000.00, '2024-11-01', TRUE);

INSERT INTO treasury_employees (full_name, employee_type, tax_id, position, doctor_fee_arrangement, hospital_commission_pct, hire_date, active)
VALUES
    ('Dr. Juan Méndez Rivera', 'DOCTOR', '8901234-5', 'Psiquiatra', 'HOSPITAL_BILLED', 15.00, '2024-01-15', TRUE),
    ('Dra. Patricia Gómez Luna', 'DOCTOR', '9012345-6', 'Médico Internista', 'HOSPITAL_BILLED', 10.00, '2024-05-01', TRUE);

-- ============================================================
-- 3. Salary History
-- ============================================================
INSERT INTO salary_history (treasury_employee_id, base_salary, effective_from, notes)
VALUES
    (1, 8500.00, '2024-03-15', 'Salario inicial'),
    (2, 12000.00, '2023-08-01', 'Salario inicial'),
    (3, 6500.00, '2025-01-10', 'Salario inicial'),
    (4, 4500.00, '2024-06-01', 'Salario inicial'),
    (5, 4200.00, '2025-06-15', 'Salario inicial');

-- ============================================================
-- 4. Expenses - January 2026
-- ============================================================
INSERT INTO expenses (supplier_name, category, description, amount, expense_date, invoice_number, status, due_date, paid_amount)
VALUES
    ('Distribuidora Médica GT', 'SUPPLIES', 'Suministros médicos enero', 12500.00, '2026-01-05', 'DM-2026-001', 'PAID', '2026-01-20', 12500.00),
    ('EEGSA', 'UTILITIES', 'Electricidad enero', 8750.00, '2026-01-10', 'EEGSA-ENE-2026', 'PAID', '2026-01-25', 8750.00),
    ('Empagua', 'UTILITIES', 'Agua enero', 2100.00, '2026-01-10', 'EMPAGUA-ENE-26', 'PAID', '2026-01-25', 2100.00),
    ('Mantenimiento Express', 'MAINTENANCE', 'Reparación aire acondicionado', 3500.00, '2026-01-15', 'ME-0045', 'PAID', '2026-01-30', 3500.00),
    ('Claro Guatemala', 'UTILITIES', 'Internet y telefonía enero', 1850.00, '2026-01-12', 'CLARO-ENE-26', 'PAID', '2026-01-27', 1850.00),
    ('Servicio de Lavandería San José', 'SERVICES', 'Lavandería hospitalaria enero', 4200.00, '2026-01-08', 'SLSJ-2026-001', 'PAID', '2026-01-23', 4200.00),
    ('Alimentos del Valle', 'SUPPLIES', 'Suministros de cocina enero', 6800.00, '2026-01-07', 'ADV-ENE-001', 'PAID', '2026-01-22', 6800.00);

-- Expenses - February 2026
INSERT INTO expenses (supplier_name, category, description, amount, expense_date, invoice_number, status, due_date, paid_amount)
VALUES
    ('Distribuidora Médica GT', 'SUPPLIES', 'Suministros médicos febrero', 14200.00, '2026-02-04', 'DM-2026-012', 'PAID', '2026-02-19', 14200.00),
    ('EEGSA', 'UTILITIES', 'Electricidad febrero', 9100.00, '2026-02-10', 'EEGSA-FEB-2026', 'PAID', '2026-02-25', 9100.00),
    ('Empagua', 'UTILITIES', 'Agua febrero', 2250.00, '2026-02-10', 'EMPAGUA-FEB-26', 'PAID', '2026-02-25', 2250.00),
    ('Claro Guatemala', 'UTILITIES', 'Internet y telefonía febrero', 1850.00, '2026-02-12', 'CLARO-FEB-26', 'PAID', '2026-02-27', 1850.00),
    ('Servicio de Lavandería San José', 'SERVICES', 'Lavandería hospitalaria febrero', 4500.00, '2026-02-06', 'SLSJ-2026-012', 'PAID', '2026-02-21', 4500.00),
    ('Alimentos del Valle', 'SUPPLIES', 'Suministros de cocina febrero', 7100.00, '2026-02-05', 'ADV-FEB-001', 'PAID', '2026-02-20', 7100.00),
    ('Equipos Médicos S.A.', 'EQUIPMENT', 'Monitor de signos vitales', 18500.00, '2026-02-15', 'EMSA-2026-003', 'PAID', '2026-03-15', 18500.00),
    ('Fumigaciones GT', 'MAINTENANCE', 'Fumigación trimestral', 2800.00, '2026-02-20', 'FGT-2026-Q1', 'PAID', '2026-03-05', 2800.00);

-- Expenses - March 2026 (some pending, some partially paid)
INSERT INTO expenses (supplier_name, category, description, amount, expense_date, invoice_number, status, due_date, paid_amount)
VALUES
    ('Distribuidora Médica GT', 'SUPPLIES', 'Suministros médicos marzo', 13800.00, '2026-03-03', 'DM-2026-023', 'PAID', '2026-03-18', 13800.00),
    ('EEGSA', 'UTILITIES', 'Electricidad marzo', 9450.00, '2026-03-10', 'EEGSA-MAR-2026', 'PENDING', '2026-03-25', 0.00),
    ('Empagua', 'UTILITIES', 'Agua marzo', 2300.00, '2026-03-10', 'EMPAGUA-MAR-26', 'PENDING', '2026-03-25', 0.00),
    ('Claro Guatemala', 'UTILITIES', 'Internet y telefonía marzo', 1850.00, '2026-03-12', 'CLARO-MAR-26', 'PENDING', '2026-03-27', 0.00),
    ('Servicio de Lavandería San José', 'SERVICES', 'Lavandería hospitalaria marzo', 4350.00, '2026-03-05', 'SLSJ-2026-023', 'PARTIALLY_PAID', '2026-03-20', 2000.00),
    ('Alimentos del Valle', 'SUPPLIES', 'Suministros de cocina marzo', 7500.00, '2026-03-04', 'ADV-MAR-001', 'PAID', '2026-03-19', 7500.00),
    ('Oxígeno Industrial GT', 'SUPPLIES', 'Tanques de oxígeno medicinal', 5600.00, '2026-03-08', 'OIGT-2026-003', 'PENDING', '2026-03-23', 0.00),
    ('Mantenimiento Express', 'MAINTENANCE', 'Mantenimiento preventivo calderas', 4200.00, '2026-03-14', 'ME-0052', 'PENDING', '2026-03-29', 0.00);

-- Expenses with future due dates (for upcoming payments report)
INSERT INTO expenses (supplier_name, category, description, amount, expense_date, invoice_number, status, due_date, paid_amount)
VALUES
    ('Seguros del País', 'SERVICES', 'Póliza de seguro trimestral', 15000.00, '2026-03-15', 'SDP-2026-Q1', 'PENDING', '2026-04-01', 0.00),
    ('Equipos Médicos S.A.', 'EQUIPMENT', 'Bomba de infusión (2 unidades)', 24000.00, '2026-03-10', 'EMSA-2026-007', 'PENDING', '2026-04-10', 0.00),
    ('Distribuidora Médica GT', 'SUPPLIES', 'Suministros médicos abril (anticipado)', 11500.00, '2026-03-18', 'DM-2026-034', 'PENDING', '2026-04-05', 0.00);

-- Contractor expenses (linked to treasury employees)
INSERT INTO expenses (supplier_name, category, description, amount, expense_date, invoice_number, status, due_date, paid_amount, treasury_employee_id)
VALUES
    ('Roberto Sánchez Morales', 'PAYROLL', 'Servicio de limpieza enero', 3500.00, '2026-01-31', 'RSM-ENE-26', 'PAID', '2026-02-05', 3500.00, 6),
    ('Servicios de Seguridad S.A.', 'PAYROLL', 'Vigilancia enero', 8000.00, '2026-01-31', 'SSA-ENE-26', 'PAID', '2026-02-05', 8000.00, 7),
    ('Roberto Sánchez Morales', 'PAYROLL', 'Servicio de limpieza febrero', 3500.00, '2026-02-28', 'RSM-FEB-26', 'PAID', '2026-03-05', 3500.00, 6),
    ('Servicios de Seguridad S.A.', 'PAYROLL', 'Vigilancia febrero', 8000.00, '2026-02-28', 'SSA-FEB-26', 'PAID', '2026-03-05', 8000.00, 7),
    ('Roberto Sánchez Morales', 'PAYROLL', 'Servicio de limpieza marzo', 3500.00, '2026-03-15', 'RSM-MAR-26', 'PENDING', '2026-03-31', 0.00, 6),
    ('Servicios de Seguridad S.A.', 'PAYROLL', 'Vigilancia marzo', 8000.00, '2026-03-15', 'SSA-MAR-26', 'PENDING', '2026-03-31', 0.00, 7);

-- ============================================================
-- 5. Expense Payments (for paid expenses) - using bank account id=2 (Cuenta Operativa)
-- ============================================================
-- January payments
INSERT INTO expense_payments (expense_id, amount, payment_date, bank_account_id, reference)
VALUES
    (1, 12500.00, '2026-01-18', 2, 'TRANSF-001'),
    (2, 8750.00, '2026-01-22', 2, 'TRANSF-002'),
    (3, 2100.00, '2026-01-22', 2, 'TRANSF-003'),
    (4, 3500.00, '2026-01-28', 2, 'TRANSF-004'),
    (5, 1850.00, '2026-01-25', 2, 'TRANSF-005'),
    (6, 4200.00, '2026-01-21', 2, 'TRANSF-006'),
    (7, 6800.00, '2026-01-20', 2, 'TRANSF-007');

-- February payments
INSERT INTO expense_payments (expense_id, amount, payment_date, bank_account_id, reference)
VALUES
    (8, 14200.00, '2026-02-17', 2, 'TRANSF-008'),
    (9, 9100.00, '2026-02-23', 2, 'TRANSF-009'),
    (10, 2250.00, '2026-02-23', 2, 'TRANSF-010'),
    (11, 1850.00, '2026-02-25', 2, 'TRANSF-011'),
    (12, 4500.00, '2026-02-19', 2, 'TRANSF-012'),
    (13, 7100.00, '2026-02-18', 2, 'TRANSF-013'),
    (14, 18500.00, '2026-03-12', 2, 'TRANSF-014'),
    (15, 2800.00, '2026-03-03', 2, 'TRANSF-015');

-- March payments (partial and full)
INSERT INTO expense_payments (expense_id, amount, payment_date, bank_account_id, reference)
VALUES
    (16, 13800.00, '2026-03-16', 2, 'TRANSF-016'),
    (20, 2000.00, '2026-03-15', 2, 'TRANSF-017'),  -- Partial payment for lavandería
    (21, 7500.00, '2026-03-17', 2, 'TRANSF-018');

-- Contractor payments (January & February)
INSERT INTO expense_payments (expense_id, amount, payment_date, bank_account_id, reference)
VALUES
    (25, 3500.00, '2026-02-03', 2, 'TRANSF-019'),
    (26, 8000.00, '2026-02-03', 2, 'TRANSF-020'),
    (27, 3500.00, '2026-03-03', 2, 'TRANSF-021'),
    (28, 8000.00, '2026-03-03', 2, 'TRANSF-022');

-- Some petty cash payments (bank account id=1)
INSERT INTO expense_payments (expense_id, amount, payment_date, bank_account_id, reference)
VALUES
    (4, 0.00, '2026-01-28', 1, 'CAJA-001');  -- This was actually paid from checking, just a placeholder
-- Let's delete that and add real petty cash expenses instead

DELETE FROM expense_payments WHERE reference = 'CAJA-001';

-- ============================================================
-- 6. Income Records
-- ============================================================
-- January income
INSERT INTO income_records (description, category, amount, income_date, reference, bank_account_id)
VALUES
    ('Pago paciente García - internamiento', 'PATIENT_PAYMENT', 25000.00, '2026-01-08', 'REC-2026-001', 2),
    ('Pago paciente Morales - consulta y tratamiento', 'PATIENT_PAYMENT', 8500.00, '2026-01-12', 'REC-2026-002', 2),
    ('Pago seguro - Paciente Hernández', 'INSURANCE_PAYMENT', 32000.00, '2026-01-15', 'SEG-2026-001', 2),
    ('Pago paciente López - hospitalización', 'PATIENT_PAYMENT', 18000.00, '2026-01-20', 'REC-2026-003', 2),
    ('Donación Fundación Esperanza', 'DONATION', 10000.00, '2026-01-25', 'DON-2026-001', 2),
    ('Subsidio MSPAS Q1', 'GOVERNMENT_SUBSIDY', 45000.00, '2026-01-28', 'GOB-2026-Q1', 2);

-- February income
INSERT INTO income_records (description, category, amount, income_date, reference, bank_account_id)
VALUES
    ('Pago paciente Díaz - internamiento', 'PATIENT_PAYMENT', 22000.00, '2026-02-03', 'REC-2026-004', 2),
    ('Pago seguro - Paciente Ramírez', 'INSURANCE_PAYMENT', 28000.00, '2026-02-10', 'SEG-2026-002', 2),
    ('Pago paciente Torres - consulta', 'PATIENT_PAYMENT', 4500.00, '2026-02-12', 'REC-2026-005', 2),
    ('Pago seguro - Paciente Flores', 'INSURANCE_PAYMENT', 35000.00, '2026-02-18', 'SEG-2026-003', 2),
    ('Pago paciente Castillo - hospitalización', 'PATIENT_PAYMENT', 15000.00, '2026-02-22', 'REC-2026-006', 2),
    ('Renta espacio consultorio externo', 'OTHER_INCOME', 6000.00, '2026-02-28', 'RENTA-FEB-26', 2);

-- March income
INSERT INTO income_records (description, category, amount, income_date, reference, bank_account_id)
VALUES
    ('Pago paciente Velásquez - internamiento', 'PATIENT_PAYMENT', 20000.00, '2026-03-02', 'REC-2026-007', 2),
    ('Pago seguro - Paciente Juárez', 'INSURANCE_PAYMENT', 40000.00, '2026-03-05', 'SEG-2026-004', 2),
    ('Pago paciente Reyes - tratamiento', 'PATIENT_PAYMENT', 12000.00, '2026-03-08', 'REC-2026-008', 2),
    ('Donación anónima', 'DONATION', 5000.00, '2026-03-10', 'DON-2026-002', 2),
    ('Pago paciente Sandoval - consulta', 'PATIENT_PAYMENT', 3500.00, '2026-03-14', 'REC-2026-009', 2),
    ('Pago seguro - Paciente Cifuentes', 'INSURANCE_PAYMENT', 27000.00, '2026-03-16', 'SEG-2026-005', 2),
    ('Renta espacio consultorio externo', 'OTHER_INCOME', 6000.00, '2026-03-01', 'RENTA-MAR-26', 2);

-- Some income to savings account
INSERT INTO income_records (description, category, amount, income_date, reference, bank_account_id)
VALUES
    ('Transferencia a ahorro - reserva', 'OTHER_INCOME', 20000.00, '2026-01-30', 'TRSF-AHO-001', 3),
    ('Transferencia a ahorro - reserva', 'OTHER_INCOME', 15000.00, '2026-02-27', 'TRSF-AHO-002', 3);

-- ============================================================
-- 7. Payroll Entries - January through March 2026
-- ============================================================
-- January payroll (all PAID)
INSERT INTO payroll_entries (treasury_employee_id, year, period, period_label, base_salary, gross_amount, due_date, status, paid_date)
VALUES
    (1, 2026, 'JANUARY', 'Enero 2026', 8500.00, 8500.00, '2026-01-31', 'PAID', '2026-01-30'),
    (2, 2026, 'JANUARY', 'Enero 2026', 12000.00, 12000.00, '2026-01-31', 'PAID', '2026-01-30'),
    (3, 2026, 'JANUARY', 'Enero 2026', 6500.00, 6500.00, '2026-01-31', 'PAID', '2026-01-30'),
    (4, 2026, 'JANUARY', 'Enero 2026', 4500.00, 4500.00, '2026-01-31', 'PAID', '2026-01-30'),
    (5, 2026, 'JANUARY', 'Enero 2026', 4200.00, 4200.00, '2026-01-31', 'PAID', '2026-01-30');

-- February payroll (all PAID)
INSERT INTO payroll_entries (treasury_employee_id, year, period, period_label, base_salary, gross_amount, due_date, status, paid_date)
VALUES
    (1, 2026, 'FEBRUARY', 'Febrero 2026', 8500.00, 8500.00, '2026-02-28', 'PAID', '2026-02-27'),
    (2, 2026, 'FEBRUARY', 'Febrero 2026', 12000.00, 12000.00, '2026-02-28', 'PAID', '2026-02-27'),
    (3, 2026, 'FEBRUARY', 'Febrero 2026', 6500.00, 6500.00, '2026-02-28', 'PAID', '2026-02-27'),
    (4, 2026, 'FEBRUARY', 'Febrero 2026', 4500.00, 4500.00, '2026-02-28', 'PAID', '2026-02-27'),
    (5, 2026, 'FEBRUARY', 'Febrero 2026', 4200.00, 4200.00, '2026-02-28', 'PAID', '2026-02-27');

-- March payroll (PENDING - upcoming)
INSERT INTO payroll_entries (treasury_employee_id, year, period, period_label, base_salary, gross_amount, due_date, status)
VALUES
    (1, 2026, 'MARCH', 'Marzo 2026', 8500.00, 8500.00, '2026-03-31', 'PENDING'),
    (2, 2026, 'MARCH', 'Marzo 2026', 12000.00, 12000.00, '2026-03-31', 'PENDING'),
    (3, 2026, 'MARCH', 'Marzo 2026', 6500.00, 6500.00, '2026-03-31', 'PENDING'),
    (4, 2026, 'MARCH', 'Marzo 2026', 4500.00, 4500.00, '2026-03-31', 'PENDING'),
    (5, 2026, 'MARCH', 'Marzo 2026', 4200.00, 4200.00, '2026-03-31', 'PENDING');

-- April payroll (PENDING - future upcoming)
INSERT INTO payroll_entries (treasury_employee_id, year, period, period_label, base_salary, gross_amount, due_date, status)
VALUES
    (1, 2026, 'APRIL', 'Abril 2026', 8500.00, 8500.00, '2026-04-30', 'PENDING'),
    (2, 2026, 'APRIL', 'Abril 2026', 12000.00, 12000.00, '2026-04-30', 'PENDING'),
    (3, 2026, 'APRIL', 'Abril 2026', 6500.00, 6500.00, '2026-04-30', 'PENDING'),
    (4, 2026, 'APRIL', 'Abril 2026', 4500.00, 4500.00, '2026-04-30', 'PENDING'),
    (5, 2026, 'APRIL', 'Abril 2026', 4200.00, 4200.00, '2026-04-30', 'PENDING');

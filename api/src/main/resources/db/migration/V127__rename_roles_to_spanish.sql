-- ============================================================================
-- V127: Rename system role codes (and display names) from English to Spanish.
--
-- The platform targets a Spanish-speaking hospital in Guatemala, and the
-- runtime now identifies system roles by their Spanish codes
-- (ADMINISTRADOR, MEDICO, ENFERMERO, ...). Historical migrations V001-V126
-- seeded the English codes (ADMIN, DOCTOR, NURSE, ...). They MUST stay
-- unchanged so their Flyway checksums keep validating on already-migrated
-- environments — see docs/architecture/MIGRATIONS.md "Modifying an
-- already-applied migration".
--
-- This forward migration performs the rename in place. It converges fresh
-- and existing databases to the same end state:
--   * fresh DB  → V001-V126 seed English codes, then this renames them.
--   * existing  → only this runs, renaming the persisted English codes.
--
-- Roles are referenced everywhere else by `roles.id` (FK), so updating
-- `roles.code` is enough for permissions, user_roles, and
-- role_default_warehouses. (The legacy `users.role` string column was dropped
-- back in V006, so there is no denormalized role string to update.)
--
-- Each UPDATE is guarded by the old code, so the migration is a harmless
-- no-op if a row was already renamed.
-- ============================================================================

-- roles.code + roles.name
UPDATE roles SET code = 'ADMINISTRADOR',          name = 'Administrador'          WHERE code = 'ADMIN';
UPDATE roles SET code = 'USUARIO',                name = 'Usuario'                WHERE code = 'USER';
UPDATE roles SET code = 'PERSONAL_ADMINISTRATIVO', name = 'Personal Administrativo' WHERE code = 'ADMINISTRATIVE_STAFF';
UPDATE roles SET code = 'MEDICO',                 name = 'Médico'                 WHERE code = 'DOCTOR';
UPDATE roles SET code = 'ENFERMERO',              name = 'Enfermero'              WHERE code = 'NURSE';
UPDATE roles SET code = 'JEFE_ENFERMERIA',        name = 'Jefe de Enfermería'     WHERE code = 'CHIEF_NURSE';
UPDATE roles SET code = 'PSICOLOGO',              name = 'Psicólogo'              WHERE code = 'PSYCHOLOGIST';
UPDATE roles SET code = 'MEDICO_RESIDENTE',       name = 'Médico Residente'       WHERE code = 'RESIDENT_DOCTOR';
UPDATE roles SET code = 'AUXILIAR_ENFERMERIA',    name = 'Auxiliar de Enfermería' WHERE code = 'AUXILIARY_NURSE';
UPDATE roles SET code = 'MANTENIMIENTO',          name = 'Mantenimiento'          WHERE code = 'MAINTENANCE';

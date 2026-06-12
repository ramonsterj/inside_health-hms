/**
 * Canonical codes for the ten seeded system roles (`roles WHERE is_system = TRUE`, Spanish codes
 * after V127). The backend mirror is `SystemRole.kt`; both are kept in lockstep with the DB by
 * coverage tests (SystemRoleCoverageTest backend, catalogs.spec.ts frontend).
 *
 * Reference these constants rather than scattering string literals so a future rename is a one-file
 * change and a typo becomes a type error.
 */
export const SYSTEM_ROLES = {
  ADMINISTRADOR: 'ADMINISTRADOR',
  USUARIO: 'USUARIO',
  PERSONAL_ADMINISTRATIVO: 'PERSONAL_ADMINISTRATIVO',
  MEDICO: 'MEDICO',
  ENFERMERO: 'ENFERMERO',
  JEFE_ENFERMERIA: 'JEFE_ENFERMERIA',
  PSICOLOGO: 'PSICOLOGO',
  MEDICO_RESIDENTE: 'MEDICO_RESIDENTE',
  AUXILIAR_ENFERMERIA: 'AUXILIAR_ENFERMERIA',
  MANTENIMIENTO: 'MANTENIMIENTO'
} as const

export type SystemRoleCode = (typeof SYSTEM_ROLES)[keyof typeof SYSTEM_ROLES]

/** All ten system-role codes — consumed by the i18n catalog guard (catalogs.ts). */
export const ROLE_CODES = Object.values(SYSTEM_ROLES) as readonly SystemRoleCode[]

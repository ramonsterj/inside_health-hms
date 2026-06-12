package com.insidehealthgt.hms.security

/**
 * Canonical codes for the ten seeded system roles (`roles WHERE is_system = TRUE`).
 *
 * These were renamed from English to Spanish in V127; the runtime identifies system roles by these
 * Spanish codes. Reference them through this object rather than as scattered string literals so a
 * future rename is a one-file change and a typo'd role check (which fails silently — always
 * false) becomes a compile-time symbol.
 *
 * Design: a plain `object` with `const val` (not an enum) because [com.insidehealthgt.hms.entity.Role.code]
 * is an open `String` namespace shared with admin-created custom roles, and `hasRole(roleCode: String)`
 * keeps its `String` signature. Feature-local policy groups (`ELEVATED_NURSING_ROLES`,
 * `MEDICAL_ORDER_BROAD_ROLES`, the warehouse role sets, …) stay in their owning companion objects,
 * rebuilt from these constants.
 *
 * [ALL] is kept in lockstep with the database by
 * [com.insidehealthgt.hms.security.SystemRoleCoverageTest].
 */
object SystemRole {
    const val ADMINISTRADOR = "ADMINISTRADOR"
    const val USUARIO = "USUARIO"
    const val PERSONAL_ADMINISTRATIVO = "PERSONAL_ADMINISTRATIVO"
    const val MEDICO = "MEDICO"
    const val ENFERMERO = "ENFERMERO"
    const val JEFE_ENFERMERIA = "JEFE_ENFERMERIA"
    const val PSICOLOGO = "PSICOLOGO"
    const val MEDICO_RESIDENTE = "MEDICO_RESIDENTE"
    const val AUXILIAR_ENFERMERIA = "AUXILIAR_ENFERMERIA"
    const val MANTENIMIENTO = "MANTENIMIENTO"

    /** All ten system-role codes. Kept in lockstep with `roles WHERE is_system = TRUE` by SystemRoleCoverageTest. */
    val ALL: Set<String> = setOf(
        ADMINISTRADOR, USUARIO, PERSONAL_ADMINISTRATIVO, MEDICO,
        ENFERMERO, JEFE_ENFERMERIA, PSICOLOGO, MEDICO_RESIDENTE, AUXILIAR_ENFERMERIA, MANTENIMIENTO,
    )
}

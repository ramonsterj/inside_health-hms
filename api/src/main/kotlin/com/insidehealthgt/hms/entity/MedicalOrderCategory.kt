package com.insidehealthgt.hms.entity

enum class MedicalOrderCategory {
    ORDENES_MEDICAS,
    MEDICAMENTOS,
    LABORATORIOS,
    REFERENCIAS_MEDICAS,
    PRUEBAS_PSICOMETRICAS,
    ACTIVIDAD_FISICA,
    CUIDADOS_ESPECIALES,
    DIETA,
    RESTRICCIONES_MOVILIDAD,
    PERMISOS_VISITA,
    OTRAS,
    ;

    fun supportsResults(): Boolean = this in RESULTS_BEARING_CATEGORIES

    fun requiresAuthorization(): Boolean = this !in DIRECTIVE_CATEGORIES

    fun initialStatus(): MedicalOrderStatus =
        if (requiresAuthorization()) MedicalOrderStatus.SOLICITADO else MedicalOrderStatus.ACTIVA

    companion object {
        val RESULTS_BEARING_CATEGORIES = setOf(LABORATORIOS, REFERENCIAS_MEDICAS, PRUEBAS_PSICOMETRICAS)

        // Directive categories don't need authorization — they're internal clinical instructions
        // that take effect on creation.
        val DIRECTIVE_CATEGORIES = setOf(
            ORDENES_MEDICAS,
            ACTIVIDAD_FISICA,
            CUIDADOS_ESPECIALES,
            DIETA,
            RESTRICCIONES_MOVILIDAD,
            PERMISOS_VISITA,
            OTRAS,
        )
    }
}

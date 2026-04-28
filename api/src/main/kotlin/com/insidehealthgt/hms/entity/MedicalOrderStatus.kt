package com.insidehealthgt.hms.entity

enum class MedicalOrderStatus {
    ACTIVA,
    SOLICITADO,
    NO_AUTORIZADO,
    AUTORIZADO,
    EN_PROCESO,
    RESULTADOS_RECIBIDOS,
    DESCONTINUADO,
    ;

    fun isTerminal(): Boolean = this in TERMINAL_STATES

    companion object {
        val TERMINAL_STATES = setOf(NO_AUTORIZADO, RESULTADOS_RECIBIDOS, DESCONTINUADO)
        val DISCONTINUABLE_STATES = setOf(ACTIVA, SOLICITADO, AUTORIZADO)
    }
}

package com.insidehealthgt.hms.entity

enum class AdmissionType {
    HOSPITALIZATION,
    AMBULATORY,
    ELECTROSHOCK_THERAPY,
    KETAMINE_INFUSION,
    EMERGENCY,
    ;

    fun requiresRoom(): Boolean = this == HOSPITALIZATION

    fun requiresTriageCode(): Boolean = this in listOf(HOSPITALIZATION, EMERGENCY)
}

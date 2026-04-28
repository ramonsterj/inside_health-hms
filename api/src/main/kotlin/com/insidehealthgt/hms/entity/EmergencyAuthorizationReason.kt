package com.insidehealthgt.hms.entity

enum class EmergencyAuthorizationReason {
    PATIENT_IN_CRISIS,
    AFTER_HOURS_NO_ADMIN,
    FAMILY_UNREACHABLE,
    OTHER,
    ;

    fun requiresNote(): Boolean = this == OTHER
}

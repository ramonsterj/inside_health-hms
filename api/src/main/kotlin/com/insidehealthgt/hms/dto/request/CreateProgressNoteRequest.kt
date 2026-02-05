package com.insidehealthgt.hms.dto.request

data class CreateProgressNoteRequest(
    val subjectiveData: String? = null,
    val objectiveData: String? = null,
    val analysis: String? = null,
    val actionPlans: String? = null,
)

package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.exception.DuplicatePatientInfo

data class DuplicatePatientResponse(val success: Boolean = false, val message: String, val data: DuplicatePatientData)

data class DuplicatePatientData(val potentialDuplicates: List<DuplicatePatientInfo>)

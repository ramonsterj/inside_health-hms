package com.insidehealthgt.hms.event

import com.insidehealthgt.hms.entity.AdmissionType

data class AdmissionCreatedEvent(val admissionId: Long, val admissionType: AdmissionType)

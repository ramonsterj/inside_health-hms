package com.insidehealthgt.hms.event

data class PatientDischargedEvent(val admissionId: Long, val patientId: Long)

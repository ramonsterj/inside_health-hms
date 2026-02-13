package com.insidehealthgt.hms.event

import java.math.BigDecimal

data class PsychotherapyActivityCreatedEvent(val admissionId: Long, val categoryName: String, val price: BigDecimal)

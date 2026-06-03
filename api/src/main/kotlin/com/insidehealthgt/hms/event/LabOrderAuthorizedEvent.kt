package com.insidehealthgt.hms.event

import java.math.BigDecimal

/**
 * Published when a `LABORATORIOS` medical order backed by the provider-catalog line model is
 * authorized. Carries the snapshotted line detail and the summed line total so billing can
 * create one [com.insidehealthgt.hms.entity.ChargeType.LAB] charge **per requested test**
 * (each itemized at its snapshotted sales price; the charges sum to [lineTotal]). Legacy lab
 * orders (single inventory item, no lines) keep publishing [MedicalOrderAuthorizedEvent] instead.
 */
data class LabOrderAuthorizedEvent(
    val admissionId: Long,
    val medicalOrderId: Long,
    val providerName: String,
    val lines: List<LabLineSnapshot>,
    val lineTotal: BigDecimal,
)

data class LabLineSnapshot(val displayName: String, val salesPrice: BigDecimal, val cost: BigDecimal)

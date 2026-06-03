package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.MedicalOrderLabTest
import java.math.BigDecimal

/** A snapshotted lab line item on a medical order response. */
data class MedicalOrderLabTestResponse(
    val id: Long,
    val labProviderTestId: Long,
    val labTestId: Long,
    val displayName: String,
    val salesPrice: BigDecimal,
    val cost: BigDecimal,
) {
    companion object {
        fun from(line: MedicalOrderLabTest) = MedicalOrderLabTestResponse(
            id = line.id!!,
            labProviderTestId = line.labProviderTestId,
            labTestId = line.labTestId,
            displayName = line.displayName,
            salesPrice = line.salesPrice,
            cost = line.cost,
        )
    }
}

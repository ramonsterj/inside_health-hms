package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.LabProviderTest
import java.math.BigDecimal

data class LabProviderTestResponse(
    val id: Long,
    val providerId: Long,
    val labTestId: Long,
    val labTestName: String,
    val displayName: String,
    val cost: BigDecimal,
    val salesPrice: BigDecimal,
    val active: Boolean,
) {
    companion object {
        fun from(providerTest: LabProviderTest) = LabProviderTestResponse(
            id = providerTest.id!!,
            providerId = providerTest.provider.id!!,
            labTestId = providerTest.labTest.id!!,
            labTestName = providerTest.labTest.name,
            displayName = providerTest.displayName,
            cost = providerTest.cost,
            salesPrice = providerTest.salesPrice,
            active = providerTest.active,
        )
    }
}

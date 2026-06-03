package com.insidehealthgt.hms.dto.response

import java.math.BigDecimal

/** A panel canonical test resolved to a provider's active offering. */
data class ResolvedPanelTest(
    val labProviderTestId: Long,
    val labTestId: Long,
    val displayName: String,
    val salesPrice: BigDecimal,
)

/** A panel canonical test the provider does not offer (not added, not billed). */
data class UnmatchedPanelTest(val labTestId: Long, val name: String)

data class PanelResolutionResponse(
    val panelId: Long,
    val providerId: Long,
    val matched: List<ResolvedPanelTest>,
    val unmatchedTests: List<UnmatchedPanelTest>,
)

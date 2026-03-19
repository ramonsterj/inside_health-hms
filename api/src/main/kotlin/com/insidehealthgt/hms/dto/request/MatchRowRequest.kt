package com.insidehealthgt.hms.dto.request

import com.insidehealthgt.hms.entity.MatchedEntityType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class MatchRowRequest(

    @field:NotNull(message = "Matched entity type is required")
    val matchedEntityType: MatchedEntityType,

    @field:NotNull(message = "Matched entity ID is required")
    @field:Positive(message = "Matched entity ID must be positive")
    val matchedEntityId: Long,
)

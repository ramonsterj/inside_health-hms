package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull

data class AssignRolesRequest(
    @field:NotNull(message = "{validation.roleCodes.notEmpty}")
    val roleCodes: List<String>,
)

package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.NotNull

data class AssignPermissionsRequest(
    @field:NotNull(message = "{validation.permissionIds.notEmpty}")
    val permissionCodes: List<String>,
)

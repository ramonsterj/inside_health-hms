package com.insidehealthgt.hms.dto.request

import jakarta.validation.constraints.Size

data class UpdateRoleRequest(
    @field:Size(max = 255, message = "{validation.role.name.size}")
    val name: String? = null,

    @field:Size(max = 500, message = "{validation.role.description.size}")
    val description: String? = null,
)

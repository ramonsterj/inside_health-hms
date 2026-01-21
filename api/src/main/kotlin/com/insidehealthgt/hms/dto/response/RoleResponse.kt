package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Role

data class RoleResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val permissions: List<PermissionResponse>,
) {
    companion object {
        fun from(role: Role): RoleResponse = RoleResponse(
            id = role.id!!,
            code = role.code,
            name = role.name,
            description = role.description,
            isSystem = role.isSystem,
            permissions = role.permissions.map { PermissionResponse.from(it) },
        )
    }
}

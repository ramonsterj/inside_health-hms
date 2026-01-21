package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.Permission

data class PermissionResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val resource: String,
    val action: String,
) {
    companion object {
        fun from(permission: Permission): PermissionResponse = PermissionResponse(
            id = permission.id!!,
            code = permission.code,
            name = permission.name,
            description = permission.description,
            resource = permission.resource,
            action = permission.action,
        )
    }
}

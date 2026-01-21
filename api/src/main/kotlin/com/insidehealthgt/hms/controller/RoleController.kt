package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AssignPermissionsRequest
import com.insidehealthgt.hms.dto.request.CreateRoleRequest
import com.insidehealthgt.hms.dto.request.UpdateRoleRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PermissionResponse
import com.insidehealthgt.hms.dto.response.RoleResponse
import com.insidehealthgt.hms.service.MessageService
import com.insidehealthgt.hms.service.RoleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/roles")
class RoleController(private val roleService: RoleService, private val messageService: MessageService) {

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    fun listRoles(): ResponseEntity<ApiResponse<List<RoleResponse>>> {
        val roles = roleService.findAll()
        return ResponseEntity.ok(ApiResponse.success(roles))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    fun getRole(@PathVariable id: Long): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleService.findById(id)
        return ResponseEntity.ok(ApiResponse.success(role))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    fun createRole(@Valid @RequestBody request: CreateRoleRequest): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(role, messageService.roleCreated()))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    fun updateRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRoleRequest,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleService.update(id, request)
        return ResponseEntity.ok(ApiResponse.success(role, messageService.roleUpdated()))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    fun deleteRole(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        roleService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(messageService.roleDeleted()))
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign-permissions')")
    fun assignPermissions(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignPermissionsRequest,
    ): ResponseEntity<ApiResponse<RoleResponse>> {
        val role = roleService.assignPermissions(id, request.permissionCodes)
        return ResponseEntity.ok(ApiResponse.success(role, messageService.rolePermissionsAssigned()))
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role:read')")
    fun listAllPermissions(): ResponseEntity<ApiResponse<List<PermissionResponse>>> {
        val permissions = roleService.findAllPermissions()
        return ResponseEntity.ok(ApiResponse.success(permissions))
    }
}

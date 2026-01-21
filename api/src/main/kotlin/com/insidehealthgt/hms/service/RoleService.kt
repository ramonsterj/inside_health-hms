package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateRoleRequest
import com.insidehealthgt.hms.dto.request.UpdateRoleRequest
import com.insidehealthgt.hms.dto.response.PermissionResponse
import com.insidehealthgt.hms.dto.response.RoleResponse
import com.insidehealthgt.hms.entity.Role
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.PermissionRepository
import com.insidehealthgt.hms.repository.RoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RoleService(private val roleRepository: RoleRepository, private val permissionRepository: PermissionRepository) {

    @Transactional(readOnly = true)
    fun findAll(): List<RoleResponse> = roleRepository.findAll().map { RoleResponse.from(it) }

    @Transactional(readOnly = true)
    fun findById(id: Long): RoleResponse {
        val role = roleRepository.findByIdWithPermissions(id)
            ?: throw ResourceNotFoundException("Role not found with id: $id")
        return RoleResponse.from(role)
    }

    @Transactional(readOnly = true)
    fun findByCode(code: String): Role = roleRepository.findByCodeWithPermissions(code)
        ?: throw ResourceNotFoundException("Role not found with code: $code")

    @Transactional
    fun create(request: CreateRoleRequest): RoleResponse {
        if (roleRepository.existsByCode(request.code)) {
            throw ConflictException("Role with code '${request.code}' already exists")
        }

        val role = Role(
            code = request.code,
            name = request.name,
            description = request.description,
            isSystem = false,
        )

        if (request.permissionCodes.isNotEmpty()) {
            val permissions = permissionRepository.findAllByCodeIn(request.permissionCodes)
            validatePermissionCodes(request.permissionCodes, permissions.map { it.code })
            role.permissions.addAll(permissions)
        }

        val savedRole = roleRepository.save(role)
        return RoleResponse.from(savedRole)
    }

    @Transactional
    fun update(id: Long, request: UpdateRoleRequest): RoleResponse {
        val role = roleRepository.findByIdWithPermissions(id)
            ?: throw ResourceNotFoundException("Role not found with id: $id")

        if (role.isSystem) {
            throw BadRequestException("System roles cannot be modified")
        }

        request.name?.let { role.name = it }
        request.description?.let { role.description = it }

        val savedRole = roleRepository.save(role)
        return RoleResponse.from(savedRole)
    }

    @Transactional
    fun delete(id: Long) {
        val role = roleRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Role not found with id: $id") }

        if (role.isSystem) {
            throw BadRequestException("System roles (ADMIN, USER) cannot be deleted")
        }

        role.deletedAt = LocalDateTime.now()
        roleRepository.save(role)
    }

    @Transactional
    fun assignPermissions(id: Long, permissionCodes: List<String>): RoleResponse {
        val role = roleRepository.findByIdWithPermissions(id)
            ?: throw ResourceNotFoundException("Role not found with id: $id")

        val permissions = permissionRepository.findAllByCodeIn(permissionCodes)
        validatePermissionCodes(permissionCodes, permissions.map { it.code })

        role.permissions.clear()
        role.permissions.addAll(permissions)

        val savedRole = roleRepository.save(role)
        return RoleResponse.from(savedRole)
    }

    @Transactional(readOnly = true)
    fun findAllPermissions(): List<PermissionResponse> =
        permissionRepository.findAll().map { PermissionResponse.from(it) }

    @Transactional(readOnly = true)
    fun findAllByCodeIn(codes: List<String>): List<Role> {
        val roles = roleRepository.findAllByCodeIn(codes)
        val foundCodes = roles.map { it.code }.toSet()
        val missingCodes = codes.filter { it !in foundCodes }
        if (missingCodes.isNotEmpty()) {
            throw BadRequestException("Invalid role codes: ${missingCodes.joinToString(", ")}")
        }
        return roles
    }

    private fun validatePermissionCodes(requested: List<String>, found: List<String>) {
        val foundCodes = found.toSet()
        val missingCodes = requested.filter { it !in foundCodes }
        if (missingCodes.isNotEmpty()) {
            throw BadRequestException("Invalid permission codes: ${missingCodes.joinToString(", ")}")
        }
    }
}

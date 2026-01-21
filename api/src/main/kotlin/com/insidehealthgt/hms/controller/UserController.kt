package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AdminUpdateUserRequest
import com.insidehealthgt.hms.dto.request.AssignRolesRequest
import com.insidehealthgt.hms.dto.request.ChangePasswordRequest
import com.insidehealthgt.hms.dto.request.CreateUserRequest
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.UserResponse
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.service.MessageService
import com.insidehealthgt.hms.service.UserService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Suppress("TooManyFunctions")
class UserController(private val userService: UserService, private val messageService: MessageService) {

    // === Current User Endpoints ===

    @GetMapping("/me")
    fun getCurrentUser(): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.getCurrentUser()
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @PutMapping("/me")
    fun updateProfile(@Valid @RequestBody request: UpdateUserRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.updateProfile(request)
        return ResponseEntity.ok(ApiResponse.success(user, messageService.userProfileUpdated()))
    }

    @PutMapping("/me/password")
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): ResponseEntity<ApiResponse<Unit>> {
        userService.changePassword(request)
        return ResponseEntity.ok(ApiResponse.success(messageService.userPasswordChanged()))
    }

    @PutMapping("/me/locale")
    fun updateLocalePreference(@RequestParam locale: String): ResponseEntity<ApiResponse<Unit>> {
        userService.updateLocalePreference(locale)
        return ResponseEntity.ok(ApiResponse.success(messageService.userLocaleUpdated()))
    }

    // === Admin User Management Endpoints ===

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    fun createUser(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user, messageService.userCreated()))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    fun listUsers(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) status: UserStatus?,
    ): ResponseEntity<ApiResponse<Page<UserResponse>>> {
        val users = userService.findAll(pageable, status)
        return ResponseEntity.ok(ApiResponse.success(users))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    fun getUser(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.findByIdWithRoles(id)
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminUpdateUserRequest,
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.updateUser(id, request)
        return ResponseEntity.ok(ApiResponse.success(user, messageService.userUpdated()))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        userService.softDelete(id)
        return ResponseEntity.ok(ApiResponse.success(messageService.userDeleted()))
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:reset-password')")
    fun resetUserPassword(@PathVariable id: Long): ResponseEntity<ApiResponse<Map<String, String>>> {
        val newPassword = userService.resetPassword(id, null)
        return ResponseEntity.ok(
            ApiResponse.success(
                mapOf("temporaryPassword" to newPassword),
                messageService.userPasswordReset(),
            ),
        )
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasAuthority('user:list-deleted')")
    fun listDeletedUsers(
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<Page<UserResponse>>> {
        val users = userService.findAllDeleted(pageable)
        return ResponseEntity.ok(ApiResponse.success(users))
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('user:restore')")
    fun restoreUser(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.restore(id)
        return ResponseEntity.ok(ApiResponse.success(user, messageService.userRestored()))
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:update')")
    fun assignRoles(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignRolesRequest,
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val user = userService.assignRoles(id, request.roleCodes)
        return ResponseEntity.ok(ApiResponse.success(user, messageService.userRolesAssigned()))
    }
}

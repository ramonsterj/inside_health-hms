package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.config.I18nConfig
import com.insidehealthgt.hms.dto.request.AdminUpdateUserRequest
import com.insidehealthgt.hms.dto.request.ChangePasswordRequest
import com.insidehealthgt.hms.dto.request.CreateUserRequest
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.dto.response.UserResponse
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): User = userRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("User not found with id: $id") }

    @Transactional(readOnly = true)
    fun findByIdWithRoles(id: Long): User = userRepository.findByIdWithRolesAndPermissions(id)
        ?: throw ResourceNotFoundException("User not found with id: $id")

    @Transactional(readOnly = true)
    fun findByEmail(email: String): User = userRepository.findByEmail(email)
        ?: throw ResourceNotFoundException("User not found with email: $email")

    @Transactional(readOnly = true)
    fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    @Transactional(readOnly = true)
    fun getCurrentUser(): UserResponse {
        val userDetails = getCurrentUserDetails()
        val user = findByIdWithRoles(userDetails.id)
        return UserResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable, status: UserStatus? = null): Page<UserResponse> {
        val users = if (status != null) {
            userRepository.findByStatus(status, pageable)
        } else {
            userRepository.findAll(pageable)
        }
        return users.map { UserResponse.from(it) }
    }

    @Transactional
    fun updateProfile(request: UpdateUserRequest): UserResponse {
        val userDetails = getCurrentUserDetails()
        val user = findByIdWithRoles(userDetails.id)

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }

        val updatedUser = userRepository.save(user)
        return UserResponse.from(updatedUser)
    }

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val userDetails = getCurrentUserDetails()
        val user = findById(userDetails.id)

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadRequestException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)!!
        userRepository.save(user)
    }

    @Transactional
    fun updateLocalePreference(locale: String) {
        val supportedLocales = I18nConfig.SUPPORTED_LOCALES.map { it.language }
        if (locale !in supportedLocales) {
            throw BadRequestException(
                messageService.errorLocaleUnsupported(locale, supportedLocales.joinToString()),
            )
        }

        val userDetails = getCurrentUserDetails()
        val user = findById(userDetails.id)
        user.localePreference = locale
        userRepository.save(user)
    }

    @Transactional
    fun softDelete(id: Long) {
        val user = findById(id)
        user.status = UserStatus.DELETED
        user.deletedAt = LocalDateTime.now()
        userRepository.save(user)
    }

    // === Admin Methods ===

    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email '${request.email}' is already registered")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("Username '${request.username}' is already taken")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)!!,
            firstName = request.firstName,
            lastName = request.lastName,
            status = request.status ?: UserStatus.ACTIVE,
            emailVerified = request.emailVerified ?: false,
        )

        // Assign roles
        val roleCodes = request.roleCodes.ifEmpty { listOf("USER") }
        val roles = roleRepository.findAllByCodeIn(roleCodes)
        validateRoleCodes(roleCodes, roles.map { it.code })
        user.roles.addAll(roles)

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun updateUser(id: Long, request: AdminUpdateUserRequest): UserResponse {
        val user = findByIdWithRoles(id)

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.status?.let { user.status = it }
        request.emailVerified?.let { user.emailVerified = it }

        // Update roles if provided
        request.roleCodes?.let { roleCodes ->
            val roles = roleRepository.findAllByCodeIn(roleCodes)
            validateRoleCodes(roleCodes, roles.map { it.code })
            user.roles.clear()
            user.roles.addAll(roles)
        }

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun resetPassword(id: Long, newPassword: String?): String {
        val user = findById(id)
        val password = newPassword ?: generateRandomPassword()
        user.passwordHash = passwordEncoder.encode(password)!!
        userRepository.save(user)
        return password
    }

    @Transactional(readOnly = true)
    fun findAllDeleted(pageable: Pageable): Page<UserResponse> =
        userRepository.findAllDeleted(pageable).map { UserResponse.from(it) }

    @Transactional
    fun restore(id: Long): UserResponse {
        val user = userRepository.findDeletedById(id)
            ?: throw ResourceNotFoundException("Deleted user not found with id: $id")

        user.status = UserStatus.ACTIVE
        user.deletedAt = null
        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun assignRoles(id: Long, roleCodes: List<String>): UserResponse {
        val user = findByIdWithRoles(id)

        val roles = roleRepository.findAllByCodeIn(roleCodes)
        validateRoleCodes(roleCodes, roles.map { it.code })

        user.roles.clear()
        user.roles.addAll(roles)

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    private fun getCurrentUserDetails(): CustomUserDetails {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException("Not authenticated")
        return authentication.principal as CustomUserDetails
    }

    private fun validateRoleCodes(requested: List<String>, found: List<String>) {
        val foundCodes = found.toSet()
        val missingCodes = requested.filter { it !in foundCodes }
        if (missingCodes.isNotEmpty()) {
            throw BadRequestException("Invalid role codes: ${missingCodes.joinToString(", ")}")
        }
    }

    private fun generateRandomPassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        val random = SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}

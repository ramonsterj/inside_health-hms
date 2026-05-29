package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.config.I18nConfig
import com.insidehealthgt.hms.dto.request.AdminUpdateUserRequest
import com.insidehealthgt.hms.dto.request.ChangePasswordRequest
import com.insidehealthgt.hms.dto.request.CreateUserRequest
import com.insidehealthgt.hms.dto.request.PhoneNumberRequest
import com.insidehealthgt.hms.dto.request.UpdateUserRequest
import com.insidehealthgt.hms.dto.response.UserResponse
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserPhoneNumber
import com.insidehealthgt.hms.entity.UserStatus
import com.insidehealthgt.hms.entity.UserWarehouse
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ConflictException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.RoleRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.UserWarehouseRepository
import com.insidehealthgt.hms.repository.WarehouseRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val currentUserProvider: CurrentUserProvider,
    private val userWarehouseRepository: UserWarehouseRepository,
    private val warehouseRepository: WarehouseRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): User = userRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("User not found with id: $id") }

    @Transactional(readOnly = true)
    fun findByIdWithRoles(id: Long): User {
        val user = userRepository.findByIdWithRolesAndPermissions(id)
            ?: throw ResourceNotFoundException("User not found with id: $id")
        // Force initialization of lazy-loaded phoneNumbers collection within the transaction.
        // Accessing .size triggers Hibernate to load the collection before the session closes.
        user.phoneNumbers.size
        return user
    }

    /**
     * Single-user response including the caller's current warehouse assignments.
     * The edit dialog preloads this before submitting, so the response MUST carry
     * the real `assignedWarehouseIds` — otherwise editing a MAINTENANCE user would
     * round-trip an empty list and wipe every assignment via [reconcileWarehouseAssignments].
     */
    @Transactional(readOnly = true)
    fun findResponseById(id: Long): UserResponse {
        val user = findByIdWithRoles(id)
        return UserResponse.from(user, currentAssignedWarehouseIds(id))
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): User = userRepository.findByEmail(email)
        ?: throw ResourceNotFoundException("User not found with email: $email")

    @Transactional(readOnly = true)
    fun isUsernameAvailable(username: String): Boolean = !userRepository.existsByUsername(username)

    @Transactional(readOnly = true)
    fun getCurrentUser(): UserResponse {
        val userDetails = currentUserProvider.currentUserDetailsOrThrow()
        val user = findByIdWithRoles(userDetails.id)
        return UserResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun findAll(
        pageable: Pageable,
        status: UserStatus? = null,
        search: String? = null,
        roleCode: String? = null,
    ): Page<UserResponse> {
        val users = if (status == null && search == null && roleCode == null) {
            userRepository.findAll(pageable)
        } else {
            userRepository.findWithFilters(
                status?.name,
                roleCode,
                search,
                pageable,
            )
        }
        return users.map { UserResponse.from(it) }
    }

    @Transactional
    fun updateProfile(request: UpdateUserRequest): UserResponse {
        val userDetails = currentUserProvider.currentUserDetailsOrThrow()
        val user = findByIdWithRoles(userDetails.id)

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }

        val updatedUser = userRepository.save(user)
        return UserResponse.from(updatedUser)
    }

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val userDetails = currentUserProvider.currentUserDetailsOrThrow()
        val user = findById(userDetails.id)

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadRequestException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)!!
        user.mustChangePassword = false
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

        val userDetails = currentUserProvider.currentUserDetailsOrThrow()
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
            salutation = request.salutation,
            status = request.status ?: UserStatus.ACTIVE,
            emailVerified = request.emailVerified ?: false,
            mustChangePassword = true,
        )

        // Assign roles
        val roleCodes = request.roleCodes.ifEmpty { listOf("USER") }
        val roles = roleRepository.findAllByCodeIn(roleCodes)
        validateRoleCodes(roleCodes, roles.map { it.code })
        user.roles.addAll(roles)

        // Add phone numbers
        addPhoneNumbers(user, request.phoneNumbers)

        val savedUser = userRepository.save(user)
        return UserResponse.from(savedUser)
    }

    @Transactional
    fun updateUser(id: Long, request: AdminUpdateUserRequest): UserResponse {
        val user = findByIdWithRoles(id)

        request.firstName?.let { user.firstName = it }
        request.lastName?.let { user.lastName = it }
        request.salutation?.let { user.salutation = it }
        request.status?.let { user.status = it }
        request.emailVerified?.let { user.emailVerified = it }

        // Update roles if provided
        request.roleCodes?.let { roleCodes ->
            val roles = roleRepository.findAllByCodeIn(roleCodes)
            validateRoleCodes(roleCodes, roles.map { it.code })
            user.roles.clear()
            user.roles.addAll(roles)
        }

        // Update phone numbers if provided (replace all)
        request.phoneNumbers?.let { phoneNumbers ->
            if (phoneNumbers.isEmpty()) {
                throw BadRequestException("At least one phone number is required")
            }
            user.phoneNumbers.clear()
            addPhoneNumbers(user, phoneNumbers)
        }

        val savedUser = userRepository.save(user)

        // Warehouse assignments only apply to MAINTENANCE users (FR-10).
        if (request.assignedWarehouseIds != null && savedUser.roles.any { it.code == MAINTENANCE_ROLE }) {
            reconcileWarehouseAssignments(savedUser, request.assignedWarehouseIds)
        }

        return UserResponse.from(savedUser, currentAssignedWarehouseIds(savedUser.id!!))
    }

    /**
     * Reconcile a MAINTENANCE user's warehouse assignments: soft-delete rows no
     * longer in the set, add new ones. Rows are never deleted on role removal —
     * only here, on an explicit assignment edit (FR-10).
     */
    private fun reconcileWarehouseAssignments(user: User, desiredIds: List<Long>) {
        val existing = userWarehouseRepository.findByUserId(user.id!!)
        val existingIds = existing.mapNotNull { it.warehouse.id }.toSet()
        val desired = desiredIds.toSet()

        existing.filter { it.warehouse.id !in desired }.forEach { row ->
            row.deletedAt = LocalDateTime.now()
            userWarehouseRepository.save(row)
        }
        desired.filter { it !in existingIds }.forEach { warehouseId ->
            val warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow { ResourceNotFoundException(messageService.errorWarehouseNotFound(warehouseId)) }
            userWarehouseRepository.save(UserWarehouse(user = user, warehouse = warehouse))
        }
    }

    private fun currentAssignedWarehouseIds(userId: Long): List<Long> =
        userWarehouseRepository.findByUserId(userId).mapNotNull { it.warehouse.id }

    @Transactional
    fun resetPassword(id: Long, newPassword: String?): String {
        val user = findById(id)
        val password = newPassword ?: generateRandomPassword()
        user.passwordHash = passwordEncoder.encode(password)!!
        user.mustChangePassword = true
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

    private fun addPhoneNumbers(user: User, phoneNumbers: List<PhoneNumberRequest>) {
        // Ensure only one primary phone number
        var hasPrimary = false
        phoneNumbers.forEach { request ->
            val phone = UserPhoneNumber(
                phoneNumber = request.phoneNumber,
                phoneType = request.phoneType,
                isPrimary = if (request.isPrimary && !hasPrimary) {
                    hasPrimary = true
                    true
                } else {
                    false
                },
            )
            user.addPhoneNumber(phone)
        }
    }

    private companion object {
        const val MAINTENANCE_ROLE = "MAINTENANCE"
    }
}

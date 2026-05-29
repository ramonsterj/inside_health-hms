package com.insidehealthgt.hms.security

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Suppress("TooManyFunctions")
class CustomUserDetails(private val user: User) : UserDetails {

    val id: Long get() = user.id!!

    val email: String get() = user.email

    val localePreference: String? get() = user.localePreference

    fun getUser(): User = user

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val authorities = mutableListOf<GrantedAuthority>()

        // Add role-based authorities (ROLE_ADMIN, ROLE_USER, etc.)
        user.roles.forEach { role ->
            authorities.add(SimpleGrantedAuthority("ROLE_${role.code}"))
        }

        // Add permission-based authorities (user:create, role:manage, etc.)
        user.getAllPermissions().forEach { permission ->
            authorities.add(SimpleGrantedAuthority(permission.code))
        }

        return authorities
    }

    fun getRoleCodes(): Set<String> = user.roles.map { it.code }.toSet()

    fun getPermissionCodes(): Set<String> = user.getAllPermissions().map { it.code }.toSet()

    fun hasPermission(permissionCode: String): Boolean = user.hasPermission(permissionCode)

    fun hasRole(roleCode: String): Boolean = user.hasRole(roleCode)

    /**
     * True when the user's only nursing-or-better role is AUXILIARY_NURSE — i.e. they hold
     * AUXILIARY_NURSE but none of NURSE / CHIEF_NURSE / DOCTOR / ADMIN. Used by the service-layer
     * guards that block auxiliary nurses from administering medications, marking medical orders in
     * progress, and uploading result documents (see docs/features/nursing-roles-split.md). Stacked
     * roles pass: a graduate nurse covering an auxiliary shift still has NURSE, so the guard lets
     * them through.
     */
    fun isAuxiliaryNurseOnly(): Boolean {
        if (!hasRole("AUXILIARY_NURSE")) return false
        return getRoleCodes().none { it in ELEVATED_NURSING_ROLES }
    }

    override fun getPassword(): String = user.passwordHash

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.SUSPENDED

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.status == UserStatus.ACTIVE

    private companion object {
        /** Roles that, when stacked with AUXILIARY_NURSE, lift the auxiliary-only restriction. */
        val ELEVATED_NURSING_ROLES = setOf("NURSE", "CHIEF_NURSE", "DOCTOR", "RESIDENT_DOCTOR", "ADMIN")
    }
}

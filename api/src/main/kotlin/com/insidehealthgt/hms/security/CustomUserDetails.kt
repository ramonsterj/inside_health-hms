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

    override fun getPassword(): String = user.passwordHash

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.SUSPENDED

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.status == UserStatus.ACTIVE
}

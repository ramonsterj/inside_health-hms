package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
class User(

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(name = "first_name", length = 100)
    var firstName: String? = null,

    @Column(name = "last_name", length = 100)
    var lastName: String? = null,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "locale_preference", length = 10)
    var localePreference: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf(),

) : BaseEntity() {

    fun getAllPermissions(): Set<Permission> = roles.flatMap { it.permissions }.toSet()

    fun hasPermission(permissionCode: String): Boolean = getAllPermissions().any { it.code == permissionCode }

    fun hasRole(roleCode: String): Boolean = roles.any { it.code == roleCode }
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DELETED,
}

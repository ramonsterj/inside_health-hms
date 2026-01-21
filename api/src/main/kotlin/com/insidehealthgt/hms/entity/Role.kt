package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "roles")
@SQLRestriction("deleted_at IS NULL")
class Role(

    @Column(nullable = false, unique = true, length = 50)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "is_system", nullable = false)
    var isSystem: Boolean = false,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")],
    )
    var permissions: MutableSet<Permission> = mutableSetOf(),

    @ManyToMany(mappedBy = "roles")
    var users: MutableSet<User> = mutableSetOf(),

) : BaseEntity()

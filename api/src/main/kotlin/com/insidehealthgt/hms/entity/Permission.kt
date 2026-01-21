package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "permissions")
@SQLRestriction("deleted_at IS NULL")
class Permission(

    @Column(nullable = false, unique = true, length = 100)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 100)
    var resource: String,

    @Column(nullable = false, length = 100)
    var action: String,

    @ManyToMany(mappedBy = "permissions")
    var roles: MutableSet<Role> = mutableSetOf(),

) : BaseEntity()

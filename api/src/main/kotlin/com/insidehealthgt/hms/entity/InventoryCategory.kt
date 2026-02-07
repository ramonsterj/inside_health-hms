package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "inventory_categories")
@SQLRestriction("deleted_at IS NULL")
class InventoryCategory(

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()

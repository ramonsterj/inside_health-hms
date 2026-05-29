package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

/**
 * A physical or logical bodega. The catalog stays single (one row per SKU);
 * stock is what becomes warehouse-scoped (see [InventoryWarehouseStock]).
 */
@Entity
@Table(name = "warehouses")
@SQLRestriction("deleted_at IS NULL")
class Warehouse(

    @Column(nullable = false, unique = true, length = 50)
    var code: String,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()

package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

@Entity
@Table(name = "inventory_items")
@SQLRestriction("deleted_at IS NULL")
class InventoryItem(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: InventoryCategory,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 12, scale = 2)
    var cost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var quantity: Int = 0,

    @Column(name = "restock_level", nullable = false)
    var restockLevel: Int = 0,

    @Column(name = "pricing_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var pricingType: PricingType = PricingType.FLAT,

    @Column(name = "time_unit", length = 20)
    @Enumerated(EnumType.STRING)
    var timeUnit: TimeUnit? = null,

    @Column(name = "time_interval")
    var timeInterval: Int? = null,

    @Column(nullable = false)
    var active: Boolean = true,

) : BaseEntity()

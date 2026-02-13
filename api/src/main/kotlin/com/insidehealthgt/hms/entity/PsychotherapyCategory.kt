package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal

@Entity
@Table(name = "psychotherapy_categories")
@SQLRestriction("deleted_at IS NULL")
class PsychotherapyCategory(

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(precision = 12, scale = 2)
    var price: BigDecimal? = null,

    @Column(precision = 12, scale = 2)
    var cost: BigDecimal? = null,

) : BaseEntity()

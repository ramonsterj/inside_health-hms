package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "triage_codes")
@SQLRestriction("deleted_at IS NULL")
class TriageCode(

    @Column(nullable = false, unique = true, length = 10)
    var code: String,

    @Column(nullable = false, length = 7)
    var color: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

) : BaseEntity()

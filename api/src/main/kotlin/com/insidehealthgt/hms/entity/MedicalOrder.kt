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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "medical_orders")
@SQLRestriction("deleted_at IS NULL")
class MedicalOrder(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var category: MedicalOrderCategory,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(length = 255)
    var medication: String? = null,

    @Column(length = 100)
    var dosage: String? = null,

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var route: AdministrationRoute? = null,

    @Column(length = 100)
    var frequency: String? = null,

    @Column(length = 100)
    var schedule: String? = null,

    @Column(columnDefinition = "TEXT")
    var observations: String? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: MedicalOrderStatus = MedicalOrderStatus.ACTIVE,

    @Column(name = "discontinued_at")
    var discontinuedAt: LocalDateTime? = null,

    @Column(name = "discontinued_by")
    var discontinuedBy: Long? = null,

) : BaseEntity()

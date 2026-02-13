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
import java.time.LocalDateTime

@Entity
@Table(name = "medication_administrations")
@SQLRestriction("deleted_at IS NULL")
class MedicationAdministration(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_order_id", nullable = false)
    var medicalOrder: MedicalOrder,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: AdministrationStatus,

    @Column(length = 1000)
    var notes: String? = null,

    @Column(name = "administered_at", nullable = false)
    var administeredAt: LocalDateTime = LocalDateTime.now(),

) : BaseEntity()

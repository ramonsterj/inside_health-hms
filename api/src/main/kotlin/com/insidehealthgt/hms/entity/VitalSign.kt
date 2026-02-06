package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "vital_signs")
@SQLRestriction("deleted_at IS NULL")
class VitalSign(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: LocalDateTime,

    @Column(name = "systolic_bp", nullable = false)
    var systolicBp: Int,

    @Column(name = "diastolic_bp", nullable = false)
    var diastolicBp: Int,

    @Column(name = "heart_rate", nullable = false)
    var heartRate: Int,

    @Column(name = "respiratory_rate", nullable = false)
    var respiratoryRate: Int,

    @Column(nullable = false, precision = 4, scale = 1)
    var temperature: BigDecimal,

    @Column(name = "oxygen_saturation", nullable = false)
    var oxygenSaturation: Int,

    @Column(length = 1000)
    var other: String? = null,

) : BaseEntity()

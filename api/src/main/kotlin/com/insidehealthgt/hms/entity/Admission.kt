package com.insidehealthgt.hms.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDateTime

@Entity
@Table(name = "admissions")
@SQLRestriction("deleted_at IS NULL")
class Admission(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triage_code_id", nullable = false)
    var triageCode: TriageCode,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: Room,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "treating_physician_id", nullable = false)
    var treatingPhysician: User,

    @Column(name = "admission_date", nullable = false)
    var admissionDate: LocalDateTime,

    @Column(name = "discharge_date")
    var dischargeDate: LocalDateTime? = null,

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var status: AdmissionStatus = AdmissionStatus.ACTIVE,

    @Column(columnDefinition = "TEXT")
    var inventory: String? = null,

    @OneToOne(mappedBy = "admission", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var consentDocument: AdmissionConsentDocument? = null,

    @OneToMany(mappedBy = "admission", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var consultingPhysicians: MutableList<AdmissionConsultingPhysician> = mutableListOf(),

) : BaseEntity() {

    fun hasConsentDocument(): Boolean = consentDocument != null

    fun isActive(): Boolean = status == AdmissionStatus.ACTIVE

    fun isDischarged(): Boolean = status == AdmissionStatus.DISCHARGED
}

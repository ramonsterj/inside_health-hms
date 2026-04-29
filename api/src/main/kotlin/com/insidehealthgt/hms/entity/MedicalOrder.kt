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
@Suppress("LongParameterList")
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

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: MedicalOrderStatus = category.initialStatus(),

    @Column(name = "authorized_at")
    var authorizedAt: LocalDateTime? = null,

    @Column(name = "authorized_by")
    var authorizedBy: Long? = null,

    @Column(name = "in_progress_at")
    var inProgressAt: LocalDateTime? = null,

    @Column(name = "in_progress_by")
    var inProgressBy: Long? = null,

    @Column(name = "results_received_at")
    var resultsReceivedAt: LocalDateTime? = null,

    @Column(name = "results_received_by")
    var resultsReceivedBy: Long? = null,

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null,

    @Column(name = "rejected_at")
    var rejectedAt: LocalDateTime? = null,

    @Column(name = "rejected_by")
    var rejectedBy: Long? = null,

    @Column(name = "emergency_authorized", nullable = false)
    var emergencyAuthorized: Boolean = false,

    @Column(name = "emergency_reason", length = 40)
    @Enumerated(EnumType.STRING)
    var emergencyReason: EmergencyAuthorizationReason? = null,

    @Column(name = "emergency_reason_note", columnDefinition = "TEXT")
    var emergencyReasonNote: String? = null,

    @Column(name = "emergency_at")
    var emergencyAt: LocalDateTime? = null,

    @Column(name = "emergency_by")
    var emergencyBy: Long? = null,

    @Column(name = "discontinued_at")
    var discontinuedAt: LocalDateTime? = null,

    @Column(name = "discontinued_by")
    var discontinuedBy: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    var inventoryItem: InventoryItem? = null,

) : BaseEntity()

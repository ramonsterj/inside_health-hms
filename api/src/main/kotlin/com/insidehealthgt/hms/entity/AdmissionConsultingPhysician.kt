package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate

@Entity
@Table(name = "admission_consulting_physicians")
@SQLRestriction("deleted_at IS NULL")
class AdmissionConsultingPhysician(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id", nullable = false)
    var admission: Admission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physician_id", nullable = false)
    var physician: User,

    @Column(length = 500)
    var reason: String? = null,

    @Column(name = "requested_date")
    var requestedDate: LocalDate? = null,

) : BaseEntity()

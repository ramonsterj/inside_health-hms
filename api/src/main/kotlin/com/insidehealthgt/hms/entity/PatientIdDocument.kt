package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "patient_id_documents")
@SQLRestriction("deleted_at IS NULL")
class PatientIdDocument(

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "file_data", nullable = false, columnDefinition = "bytea")
    var fileData: ByteArray,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    var patient: Patient? = null,

) : BaseEntity() {

    companion object {
        const val MAX_FILE_SIZE: Long = 5 * 1024 * 1024 // 5MB
        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
        )
    }
}

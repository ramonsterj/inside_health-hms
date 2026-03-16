package com.insidehealthgt.hms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "medical_order_documents")
@SQLRestriction("deleted_at IS NULL")
class MedicalOrderDocument(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_order_id", nullable = false)
    var medicalOrder: MedicalOrder,

    @Column(name = "display_name", nullable = false, length = 255)
    var displayName: String,

    @Column(name = "file_name", nullable = false, length = 255)
    var fileName: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "file_size", nullable = false)
    var fileSize: Long,

    @Column(name = "storage_path", nullable = false, length = 500)
    var storagePath: String,

    @Column(name = "thumbnail_path", length = 500)
    var thumbnailPath: String? = null,

) : BaseEntity() {

    fun hasThumbnail(): Boolean = thumbnailPath != null

    companion object {
        const val MAX_FILE_SIZE: Long = 25 * 1024 * 1024 // 25MB
        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "application/pdf",
        )
    }
}

package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.MedicalOrderDocument
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class MedicalOrderDocumentResponse(
    val id: Long,
    val displayName: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val hasThumbnail: Boolean,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
    val createdAt: LocalDateTime?,
    val createdBy: MedicalStaffResponse?,
) {
    companion object {
        fun from(
            document: MedicalOrderDocument,
            admissionId: Long,
            createdByUser: User? = null,
        ): MedicalOrderDocumentResponse {
            val orderId = document.medicalOrder.id
            val documentId = document.id

            return MedicalOrderDocumentResponse(
                id = document.id!!,
                displayName = document.displayName,
                fileName = document.fileName,
                contentType = document.contentType,
                fileSize = document.fileSize,
                hasThumbnail = document.hasThumbnail(),
                thumbnailUrl = if (document.hasThumbnail()) {
                    "/v1/admissions/$admissionId/medical-orders/$orderId/documents/$documentId/thumbnail"
                } else {
                    null
                },
                downloadUrl = "/v1/admissions/$admissionId/medical-orders/$orderId/documents/$documentId/file",
                createdAt = document.createdAt,
                createdBy = createdByUser?.let { MedicalStaffResponse.from(it) },
            )
        }
    }
}

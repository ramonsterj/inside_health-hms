package com.insidehealthgt.hms.dto.response

import com.insidehealthgt.hms.entity.AdmissionDocument
import com.insidehealthgt.hms.entity.User
import java.time.LocalDateTime

data class AdmissionDocumentResponse(
    val id: Long,
    val documentType: DocumentTypeSummaryResponse,
    val displayName: String,
    val fileName: String,
    val contentType: String,
    val fileSize: Long,
    val hasThumbnail: Boolean,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
    val createdAt: LocalDateTime?,
    val createdBy: UserSummaryResponse?,
) {
    companion object {
        fun from(document: AdmissionDocument, createdByUser: User? = null): AdmissionDocumentResponse {
            val admissionId = document.admission.id
            val documentId = document.id

            return AdmissionDocumentResponse(
                id = document.id!!,
                documentType = DocumentTypeSummaryResponse.from(document.documentType),
                displayName = document.displayName,
                fileName = document.fileName,
                contentType = document.contentType,
                fileSize = document.fileSize,
                hasThumbnail = document.hasThumbnail(),
                thumbnailUrl = if (document.hasThumbnail()) {
                    "/v1/admissions/$admissionId/documents/$documentId/thumbnail"
                } else {
                    null
                },
                downloadUrl = "/v1/admissions/$admissionId/documents/$documentId/file",
                createdAt = document.createdAt,
                createdBy = createdByUser?.let { UserSummaryResponse.from(it) },
            )
        }
    }
}

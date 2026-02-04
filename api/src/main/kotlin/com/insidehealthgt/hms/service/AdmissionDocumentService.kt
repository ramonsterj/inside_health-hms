package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.AdmissionDocumentResponse
import com.insidehealthgt.hms.entity.AdmissionDocument
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.DocumentTypeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class AdmissionDocumentService(
    private val admissionDocumentRepository: AdmissionDocumentRepository,
    private val admissionRepository: AdmissionRepository,
    private val documentTypeRepository: DocumentTypeRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
    private val thumbnailService: ThumbnailService,
) {

    @Transactional
    fun uploadDocument(
        admissionId: Long,
        documentTypeId: Long,
        displayName: String?,
        file: MultipartFile,
    ): AdmissionDocumentResponse {
        val admission = admissionRepository.findById(admissionId)
            .orElseThrow { ResourceNotFoundException("Admission not found with id: $admissionId") }

        val documentType = documentTypeRepository.findById(documentTypeId)
            .orElseThrow { ResourceNotFoundException("Document type not found with id: $documentTypeId") }

        validateFile(file)

        val storagePath = fileStorageService.storeFileForAdmission(admissionId, file)
        val thumbnailPath = generateAndStoreThumbnail(admissionId, storagePath, file.contentType ?: "")
        val finalDisplayName = if (displayName.isNullOrBlank()) {
            file.originalFilename ?: "document"
        } else {
            displayName
        }

        val document = AdmissionDocument(
            admission = admission,
            documentType = documentType,
            displayName = finalDisplayName,
            fileName = file.originalFilename ?: "document",
            contentType = file.contentType ?: "application/octet-stream",
            fileSize = file.size,
            storagePath = storagePath,
            thumbnailPath = thumbnailPath,
        )

        val savedDocument = admissionDocumentRepository.save(document)
        return buildAdmissionDocumentResponse(savedDocument)
    }

    @Transactional(readOnly = true)
    fun listDocuments(admissionId: Long): List<AdmissionDocumentResponse> {
        // Verify admission exists
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }

        val documents = admissionDocumentRepository.findByAdmissionIdWithDocumentType(admissionId)

        // Batch fetch createdBy users
        val createdByIds = documents.mapNotNull { it.createdBy }.distinct()
        val createdByUsers = if (createdByIds.isNotEmpty()) {
            userRepository.findAllById(createdByIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return documents.map { doc ->
            val createdByUser = doc.createdBy?.let { createdByUsers[it] }
            AdmissionDocumentResponse.from(doc, createdByUser)
        }
    }

    @Transactional(readOnly = true)
    fun getDocument(admissionId: Long, documentId: Long): AdmissionDocumentResponse {
        val document = findByIdAndAdmissionId(documentId, admissionId)
        return buildAdmissionDocumentResponse(document)
    }

    @Transactional(readOnly = true)
    fun downloadDocument(admissionId: Long, documentId: Long): DocumentFileData {
        val document = findByIdAndAdmissionId(documentId, admissionId)

        val fileData = fileStorageService.loadFile(document.storagePath)

        return DocumentFileData(
            fileName = document.displayName,
            contentType = document.contentType,
            fileSize = document.fileSize,
            fileData = fileData,
        )
    }

    @Transactional(readOnly = true)
    fun getThumbnail(admissionId: Long, documentId: Long): DocumentFileData? {
        val document = findByIdAndAdmissionId(documentId, admissionId)

        if (document.thumbnailPath == null) {
            return null
        }

        val fileData = fileStorageService.loadFile(document.thumbnailPath!!)

        return DocumentFileData(
            fileName = "${document.displayName}$THUMBNAIL_SUFFIX",
            contentType = "image/png",
            fileSize = fileData.size.toLong(),
            fileData = fileData,
        )
    }

    @Transactional
    fun deleteDocument(admissionId: Long, documentId: Long) {
        val document = findByIdAndAdmissionId(documentId, admissionId)
        document.deletedAt = LocalDateTime.now()
        admissionDocumentRepository.save(document)
    }

    private fun findByIdAndAdmissionId(documentId: Long, admissionId: Long): AdmissionDocument =
        admissionDocumentRepository.findByIdAndAdmissionIdWithDocumentType(documentId, admissionId)
            ?: throw ResourceNotFoundException("Document not found with id: $documentId for admission: $admissionId")

    private fun validateFile(file: MultipartFile) {
        val error = when {
            file.isEmpty -> "File is empty"

            file.size > AdmissionDocument.MAX_FILE_SIZE ->
                "The file is too large. Maximum allowed size is 25MB."

            file.contentType == null || file.contentType !in AdmissionDocument.ALLOWED_CONTENT_TYPES ->
                "This file type is not supported. Please upload a PDF, JPEG, or PNG file."

            else -> null
        }
        error?.let { throw BadRequestException(it) }
    }

    private fun generateAndStoreThumbnail(admissionId: Long, storagePath: String, contentType: String): String? {
        val absolutePath = fileStorageService.getAbsolutePath(storagePath)
        val thumbnailBytes = thumbnailService.generateThumbnail(absolutePath, contentType)
            ?: return null

        // Generate thumbnail path based on original file path
        val originalFilename = Paths.get(storagePath).fileName.toString()
        val thumbnailFilename = "${UUID.randomUUID()}_${originalFilename}$THUMBNAIL_SUFFIX"

        val thumbnailRelativePath = Paths.get(
            "admissions",
            admissionId.toString(),
            "documents",
            thumbnailFilename,
        )

        return fileStorageService.storeBytes(thumbnailRelativePath, thumbnailBytes)
    }

    private fun buildAdmissionDocumentResponse(document: AdmissionDocument): AdmissionDocumentResponse {
        val createdByUser = document.createdBy?.let { userRepository.findById(it).orElse(null) }
        return AdmissionDocumentResponse.from(document, createdByUser)
    }

    companion object {
        const val THUMBNAIL_SUFFIX = "_thumb.png"
    }
}

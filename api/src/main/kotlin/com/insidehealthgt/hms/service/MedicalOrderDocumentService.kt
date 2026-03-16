package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.MedicalOrderDocumentResponse
import com.insidehealthgt.hms.entity.MedicalOrderDocument
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.MedicalOrderDocumentRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.UUID

@Service
class MedicalOrderDocumentService(
    private val medicalOrderDocumentRepository: MedicalOrderDocumentRepository,
    private val medicalOrderRepository: MedicalOrderRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
    private val thumbnailService: ThumbnailService,
    private val messageService: MessageService,
) {

    @Transactional
    fun uploadDocument(
        admissionId: Long,
        orderId: Long,
        displayName: String?,
        file: MultipartFile,
    ): MedicalOrderDocumentResponse {
        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        validateFile(file)

        val storagePath = fileStorageService.storeFileForMedicalOrder(admissionId, orderId, file)
        val thumbnailPath = generateAndStoreThumbnail(admissionId, orderId, storagePath, file.contentType ?: "")
        val finalDisplayName = if (displayName.isNullOrBlank()) {
            file.originalFilename ?: "document"
        } else {
            displayName
        }

        val document = MedicalOrderDocument(
            medicalOrder = order,
            displayName = finalDisplayName,
            fileName = file.originalFilename ?: "document",
            contentType = file.contentType ?: "application/octet-stream",
            fileSize = file.size,
            storagePath = storagePath,
            thumbnailPath = thumbnailPath,
        )

        val savedDocument = medicalOrderDocumentRepository.save(document)
        return buildResponse(savedDocument, admissionId)
    }

    @Transactional(readOnly = true)
    fun listDocuments(admissionId: Long, orderId: Long): List<MedicalOrderDocumentResponse> {
        // Verify order belongs to admission
        medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        val documents = medicalOrderDocumentRepository.findByMedicalOrderIdOrderByCreatedAtDesc(orderId)

        // Batch fetch createdBy users
        val createdByIds = documents.mapNotNull { it.createdBy }.distinct()
        val createdByUsers = if (createdByIds.isNotEmpty()) {
            userRepository.findAllById(createdByIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return documents.map { doc ->
            val createdByUser = doc.createdBy?.let { createdByUsers[it] }
            MedicalOrderDocumentResponse.from(doc, admissionId, createdByUser)
        }
    }

    @Transactional(readOnly = true)
    fun downloadDocument(admissionId: Long, orderId: Long, documentId: Long): DocumentFileData {
        val document = findByIdAndOrderId(documentId, orderId, admissionId)

        val fileData = fileStorageService.loadFile(document.storagePath)

        return DocumentFileData(
            fileName = document.displayName,
            contentType = document.contentType,
            fileSize = document.fileSize,
            fileData = fileData,
        )
    }

    @Transactional(readOnly = true)
    fun getThumbnail(admissionId: Long, orderId: Long, documentId: Long): DocumentFileData? {
        val document = findByIdAndOrderId(documentId, orderId, admissionId)

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
    fun deleteDocument(admissionId: Long, orderId: Long, documentId: Long) {
        val document = findByIdAndOrderId(documentId, orderId, admissionId)
        document.deletedAt = LocalDateTime.now()
        medicalOrderDocumentRepository.save(document)
    }

    private fun findByIdAndOrderId(documentId: Long, orderId: Long, admissionId: Long): MedicalOrderDocument {
        // Verify order belongs to admission
        medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        return medicalOrderDocumentRepository.findByIdAndMedicalOrderId(documentId, orderId)
            ?: throw ResourceNotFoundException("Document not found with id: $documentId for order: $orderId")
    }

    private fun validateFile(file: MultipartFile) {
        val error = when {
            file.isEmpty -> messageService.errorMedicalOrderDocumentFileEmpty()

            file.size > MedicalOrderDocument.MAX_FILE_SIZE ->
                messageService.errorMedicalOrderDocumentFileSize()

            file.contentType == null || file.contentType !in MedicalOrderDocument.ALLOWED_CONTENT_TYPES ->
                messageService.errorMedicalOrderDocumentFileType()

            else -> null
        }
        error?.let { throw BadRequestException(it) }
    }

    private fun generateAndStoreThumbnail(
        admissionId: Long,
        orderId: Long,
        storagePath: String,
        contentType: String,
    ): String? {
        val absolutePath = fileStorageService.getAbsolutePath(storagePath)
        val thumbnailBytes = thumbnailService.generateThumbnail(absolutePath, contentType)
            ?: return null

        val originalFilename = Paths.get(storagePath).fileName.toString()
        val thumbnailFilename = "${UUID.randomUUID()}_${originalFilename}$THUMBNAIL_SUFFIX"

        val thumbnailRelativePath = Paths.get(
            "admissions",
            admissionId.toString(),
            "medical-orders",
            orderId.toString(),
            thumbnailFilename,
        )

        return fileStorageService.storeBytes(thumbnailRelativePath, thumbnailBytes)
    }

    private fun buildResponse(document: MedicalOrderDocument, admissionId: Long): MedicalOrderDocumentResponse {
        val createdByUser = document.createdBy?.let { userRepository.findById(it).orElse(null) }
        return MedicalOrderDocumentResponse.from(document, admissionId, createdByUser)
    }

    companion object {
        const val THUMBNAIL_SUFFIX = "_thumb.png"
    }
}

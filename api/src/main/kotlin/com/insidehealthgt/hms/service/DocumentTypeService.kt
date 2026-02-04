package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateDocumentTypeRequest
import com.insidehealthgt.hms.dto.request.UpdateDocumentTypeRequest
import com.insidehealthgt.hms.dto.response.DocumentTypeResponse
import com.insidehealthgt.hms.dto.response.DocumentTypeSummaryResponse
import com.insidehealthgt.hms.entity.DocumentType
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.DocumentTypeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DocumentTypeService(
    private val documentTypeRepository: DocumentTypeRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): DocumentType = documentTypeRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Document type not found with id: $id") }

    @Transactional(readOnly = true)
    fun findByCode(code: String): DocumentType = documentTypeRepository.findByCode(code)
        ?: throw ResourceNotFoundException("Document type not found with code: $code")

    @Transactional(readOnly = true)
    fun findAll(): List<DocumentTypeResponse> = documentTypeRepository.findAllByOrderByDisplayOrderAsc()
        .map { DocumentTypeResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun findAllSummary(): List<DocumentTypeSummaryResponse> = documentTypeRepository.findAllByOrderByDisplayOrderAsc()
        .map { DocumentTypeSummaryResponse.from(it) }

    @Transactional(readOnly = true)
    fun getDocumentType(id: Long): DocumentTypeResponse {
        val documentType = findById(id)
        return buildDocumentTypeResponse(documentType)
    }

    @Transactional
    fun createDocumentType(request: CreateDocumentTypeRequest): DocumentTypeResponse {
        if (documentTypeRepository.existsByCode(request.code)) {
            throw BadRequestException("Document type with code '${request.code}' already exists")
        }

        val documentType = DocumentType(
            code = request.code,
            name = request.name,
            description = request.description,
            displayOrder = request.displayOrder,
        )

        val savedDocumentType = documentTypeRepository.save(documentType)
        return buildDocumentTypeResponse(savedDocumentType)
    }

    @Transactional
    fun updateDocumentType(id: Long, request: UpdateDocumentTypeRequest): DocumentTypeResponse {
        val documentType = findById(id)

        if (documentTypeRepository.existsByCodeExcludingId(request.code, id)) {
            throw BadRequestException("Document type with code '${request.code}' already exists")
        }

        documentType.code = request.code
        documentType.name = request.name
        documentType.description = request.description
        documentType.displayOrder = request.displayOrder

        val savedDocumentType = documentTypeRepository.save(documentType)
        return buildDocumentTypeResponse(savedDocumentType)
    }

    @Transactional
    fun deleteDocumentType(id: Long) {
        val documentType = findById(id)

        if (documentTypeRepository.hasDocuments(id)) {
            throw BadRequestException("Cannot delete document type. Documents of this type exist.")
        }

        documentType.deletedAt = LocalDateTime.now()
        documentTypeRepository.save(documentType)
    }

    private fun buildDocumentTypeResponse(documentType: DocumentType): DocumentTypeResponse {
        val createdByUser = documentType.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = documentType.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return DocumentTypeResponse.from(documentType, createdByUser, updatedByUser)
    }
}

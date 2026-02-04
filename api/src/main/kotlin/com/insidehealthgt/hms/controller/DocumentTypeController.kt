package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreateDocumentTypeRequest
import com.insidehealthgt.hms.dto.request.UpdateDocumentTypeRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.DocumentTypeResponse
import com.insidehealthgt.hms.dto.response.DocumentTypeSummaryResponse
import com.insidehealthgt.hms.service.DocumentTypeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/document-types")
class DocumentTypeController(private val documentTypeService: DocumentTypeService) {

    @GetMapping
    @PreAuthorize("hasAuthority('document-type:read')")
    fun listDocumentTypes(): ResponseEntity<ApiResponse<List<DocumentTypeResponse>>> {
        val documentTypes = documentTypeService.findAll()
        return ResponseEntity.ok(ApiResponse.success(documentTypes))
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('document-type:read')")
    fun listDocumentTypesSummary(): ResponseEntity<ApiResponse<List<DocumentTypeSummaryResponse>>> {
        val documentTypes = documentTypeService.findAllSummary()
        return ResponseEntity.ok(ApiResponse.success(documentTypes))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document-type:read')")
    fun getDocumentType(@PathVariable id: Long): ResponseEntity<ApiResponse<DocumentTypeResponse>> {
        val documentType = documentTypeService.getDocumentType(id)
        return ResponseEntity.ok(ApiResponse.success(documentType))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('document-type:create')")
    fun createDocumentType(
        @Valid @RequestBody request: CreateDocumentTypeRequest,
    ): ResponseEntity<ApiResponse<DocumentTypeResponse>> {
        val documentType = documentTypeService.createDocumentType(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(documentType))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('document-type:update')")
    fun updateDocumentType(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateDocumentTypeRequest,
    ): ResponseEntity<ApiResponse<DocumentTypeResponse>> {
        val documentType = documentTypeService.updateDocumentType(id, request)
        return ResponseEntity.ok(ApiResponse.success(documentType))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('document-type:delete')")
    fun deleteDocumentType(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        documentTypeService.deleteDocumentType(id)
        return ResponseEntity.ok(ApiResponse.success("Document type deleted successfully"))
    }
}

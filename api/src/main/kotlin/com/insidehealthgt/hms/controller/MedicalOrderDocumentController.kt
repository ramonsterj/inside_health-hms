package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderDocumentResponse
import com.insidehealthgt.hms.service.MedicalOrderDocumentService
import com.insidehealthgt.hms.service.MessageService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/medical-orders/{orderId}/documents")
class MedicalOrderDocumentController(
    private val medicalOrderDocumentService: MedicalOrderDocumentService,
    private val messageService: MessageService,
) {

    @GetMapping
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun listDocuments(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
    ): ResponseEntity<ApiResponse<List<MedicalOrderDocumentResponse>>> {
        val documents = medicalOrderDocumentService.listDocuments(admissionId, orderId)
        return ResponseEntity.ok(ApiResponse.success(documents))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('medical-order:upload-document')")
    fun uploadDocument(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "displayName", required = false) displayName: String?,
    ): ResponseEntity<ApiResponse<MedicalOrderDocumentResponse>> {
        val document = medicalOrderDocumentService.uploadDocument(admissionId, orderId, displayName, file)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document))
    }

    @GetMapping("/{docId}/file")
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun downloadDocument(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @PathVariable docId: Long,
    ): ResponseEntity<ByteArray> {
        val document = medicalOrderDocumentService.downloadDocument(admissionId, orderId, docId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .contentType(MediaType.parseMediaType(document.contentType))
            .contentLength(document.fileSize)
            .body(document.fileData)
    }

    @GetMapping("/{docId}/thumbnail")
    @PreAuthorize("hasAuthority('medical-order:read')")
    fun getThumbnail(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @PathVariable docId: Long,
    ): ResponseEntity<ByteArray> {
        val thumbnail = medicalOrderDocumentService.getThumbnail(admissionId, orderId, docId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(thumbnail.fileData)
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasAuthority('medical-order:delete-document')")
    fun deleteDocument(
        @PathVariable admissionId: Long,
        @PathVariable orderId: Long,
        @PathVariable docId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        medicalOrderDocumentService.deleteDocument(admissionId, orderId, docId)
        return ResponseEntity.ok(ApiResponse.success(messageService.medicalOrderDocumentDeleted()))
    }
}

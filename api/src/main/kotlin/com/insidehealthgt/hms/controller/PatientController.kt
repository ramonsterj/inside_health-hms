package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.UpdatePatientRequest
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.PatientResponse
import com.insidehealthgt.hms.dto.response.PatientSummaryResponse
import com.insidehealthgt.hms.service.PatientService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/patients")
class PatientController(private val patientService: PatientService) {

    @PostMapping
    @PreAuthorize("hasAuthority('patient:create')")
    fun createPatient(
        @Valid @RequestBody request: CreatePatientRequest,
    ): ResponseEntity<ApiResponse<PatientResponse>> {
        val patient = patientService.createPatient(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(patient))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('patient:read')")
    fun listPatients(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<ApiResponse<Page<PatientSummaryResponse>>> {
        val patients = patientService.findAll(pageable, search)
        return ResponseEntity.ok(ApiResponse.success(patients))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:read')")
    fun getPatient(@PathVariable id: Long): ResponseEntity<ApiResponse<PatientResponse>> {
        val patient = patientService.getPatient(id)
        return ResponseEntity.ok(ApiResponse.success(patient))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:update')")
    fun updatePatient(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePatientRequest,
    ): ResponseEntity<ApiResponse<PatientResponse>> {
        val patient = patientService.updatePatient(id, request)
        return ResponseEntity.ok(ApiResponse.success(patient))
    }

    @PostMapping("/{id}/id-document")
    @PreAuthorize("hasAuthority('patient:upload-id')")
    fun uploadIdDocument(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse<PatientResponse>> {
        val patient = patientService.uploadIdDocument(id, file)
        return ResponseEntity.ok(ApiResponse.success(patient))
    }

    @GetMapping("/{id}/id-document")
    @PreAuthorize("hasAuthority('patient:view-id')")
    fun downloadIdDocument(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val document = patientService.getIdDocument(id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .contentType(MediaType.parseMediaType(document.contentType))
            .contentLength(document.fileSize)
            .body(document.fileData)
    }

    @DeleteMapping("/{id}/id-document")
    @PreAuthorize("hasAuthority('patient:upload-id')")
    fun deleteIdDocument(@PathVariable id: Long): ResponseEntity<ApiResponse<PatientResponse>> {
        val patient = patientService.deleteIdDocument(id)
        return ResponseEntity.ok(ApiResponse.success(patient))
    }
}

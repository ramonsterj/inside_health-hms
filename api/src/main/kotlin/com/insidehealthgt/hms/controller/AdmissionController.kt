package com.insidehealthgt.hms.controller

import com.insidehealthgt.hms.dto.request.AddConsultingPhysicianRequest
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.DischargeAdmissionRequest
import com.insidehealthgt.hms.dto.request.UpdateAdmissionRequest
import com.insidehealthgt.hms.dto.response.AdmissionDetailResponse
import com.insidehealthgt.hms.dto.response.AdmissionDocumentResponse
import com.insidehealthgt.hms.dto.response.AdmissionListResponse
import com.insidehealthgt.hms.dto.response.ApiResponse
import com.insidehealthgt.hms.dto.response.ConsultingPhysicianResponse
import com.insidehealthgt.hms.dto.response.DoctorResponse
import com.insidehealthgt.hms.dto.response.PageResponse
import com.insidehealthgt.hms.dto.response.PatientSummaryResponse
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.security.CustomUserDetails
import com.insidehealthgt.hms.security.SystemRole
import com.insidehealthgt.hms.service.AdmissionDocumentService
import com.insidehealthgt.hms.service.AdmissionService
import com.insidehealthgt.hms.service.MessageService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
@RequestMapping("/api/v1/admissions")
@Suppress("TooManyFunctions")
class AdmissionController(
    private val admissionService: AdmissionService,
    private val admissionDocumentService: AdmissionDocumentService,
    private val messageService: MessageService,
) {

    @GetMapping
    @PreAuthorize("hasAuthority('admission:read')")
    fun listAdmissions(
        @PageableDefault(size = 20) pageable: Pageable,
        @RequestParam(required = false) status: AdmissionStatus?,
        @RequestParam(required = false) type: AdmissionType?,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<PageResponse<AdmissionListResponse>>> {
        val doctorId = resolveDoctorId(currentUser)
        val effectiveStatus = if (resolveActiveAdmissionsOnly(currentUser)) AdmissionStatus.ACTIVE else status
        val admissions = admissionService.findAll(pageable, effectiveStatus, type, doctorId)
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(admissions)))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admission:read')")
    fun getAdmission(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<AdmissionDetailResponse>> {
        val admission = admissionService.getAdmission(id, resolveActiveAdmissionsOnly(currentUser))
        return ResponseEntity.ok(ApiResponse.success(admission))
    }

    // Only the standalone MEDICO role is scoped to own patients. A user who also
    // carries MEDICO_RESIDENTE (or ADMINISTRADOR) must see every admission, since residents
    // run the full ward — not just the patients they personally admitted.
    private fun resolveDoctorId(currentUser: CustomUserDetails): Long? {
        val isStandaloneDoctor = currentUser.hasRole(SystemRole.MEDICO) &&
            !currentUser.hasRole(SystemRole.ADMINISTRADOR) &&
            !currentUser.hasRole(SystemRole.MEDICO_RESIDENTE)
        return if (isStandaloneDoctor) currentUser.id else null
    }

    private fun resolveActiveAdmissionsOnly(currentUser: CustomUserDetails): Boolean =
        currentUser.hasRole(SystemRole.PSICOLOGO) && !currentUser.hasRole(SystemRole.ADMINISTRADOR)

    @PostMapping
    @PreAuthorize("hasAuthority('admission:create')")
    fun createAdmission(
        @Valid @RequestBody request: CreateAdmissionRequest,
    ): ResponseEntity<ApiResponse<AdmissionDetailResponse>> {
        val admission = admissionService.createAdmission(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(admission))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admission:update')")
    fun updateAdmission(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAdmissionRequest,
    ): ResponseEntity<ApiResponse<AdmissionDetailResponse>> {
        val admission = admissionService.updateAdmission(id, request)
        return ResponseEntity.ok(ApiResponse.success(admission))
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAuthority('admission:discharge')")
    fun dischargePatient(
        @PathVariable id: Long,
        @RequestBody(required = false) request: DischargeAdmissionRequest?,
    ): ResponseEntity<ApiResponse<AdmissionDetailResponse>> {
        val admission = admissionService.dischargePatient(id, request ?: DischargeAdmissionRequest())
        return ResponseEntity.ok(ApiResponse.success(admission))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admission:delete')")
    fun deleteAdmission(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        admissionService.deleteAdmission(id)
        return ResponseEntity.ok(ApiResponse.success(messageService.admissionDeleted()))
    }

    @PostMapping("/{id}/consent")
    @PreAuthorize("hasAuthority('admission:upload-consent')")
    fun uploadConsentDocument(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<ApiResponse<AdmissionDetailResponse>> {
        val admission = admissionService.uploadConsentDocument(id, file)
        return ResponseEntity.ok(ApiResponse.success(admission))
    }

    @GetMapping("/{id}/consent")
    @PreAuthorize("hasAuthority('admission:view-consent')")
    fun downloadConsentDocument(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val document = admissionService.getConsentDocument(id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .contentType(MediaType.parseMediaType(document.contentType))
            .contentLength(document.fileSize)
            .body(document.fileData)
    }

    @GetMapping("/patients/search")
    @PreAuthorize("hasAuthority('patient:read')")
    fun searchPatients(
        @RequestParam q: String,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<PatientSummaryResponse>>> {
        val patients = admissionService.searchPatients(q, resolveActiveAdmissionsOnly(currentUser))
        return ResponseEntity.ok(ApiResponse.success(patients))
    }

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAuthority('patient:read')")
    fun getPatientSummary(
        @PathVariable patientId: Long,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<PatientSummaryResponse>> {
        val patient = admissionService.getPatientSummary(patientId, resolveActiveAdmissionsOnly(currentUser))
        return ResponseEntity.ok(ApiResponse.success(patient))
    }

    /**
     * Lists all admissions for a single patient, most-recent-first.
     *
     * Gate-not-filter: [resolveDoctorId] / [resolveActiveAdmissionsOnly] are passed in ONLY to
     * authorize access to the patient (the same visibility gate as the patient detail page) — they
     * must NOT be wired into the admission query as per-row filters. Returns 403 if the caller
     * cannot open the patient, 404 if the patient does not exist.
     */
    @GetMapping("/patients/{patientId}/admissions")
    @PreAuthorize("hasAuthority('patient:read')")
    fun listPatientAdmissions(
        @PathVariable patientId: Long,
        @PageableDefault(size = 20) pageable: Pageable,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<PageResponse<AdmissionListResponse>>> {
        val admissions = admissionService.findAdmissionsByPatient(
            patientId = patientId,
            doctorId = resolveDoctorId(currentUser),
            activeAdmissionsOnly = resolveActiveAdmissionsOnly(currentUser),
            pageable = pageable,
        )
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(admissions)))
    }

    @GetMapping("/doctors")
    @PreAuthorize("hasAuthority('admission:create')")
    fun listDoctors(): ResponseEntity<ApiResponse<List<DoctorResponse>>> {
        val doctors = admissionService.listDoctors()
        return ResponseEntity.ok(ApiResponse.success(doctors))
    }

    @GetMapping("/residents")
    @PreAuthorize("hasAuthority('admission:create')")
    fun listResidents(): ResponseEntity<ApiResponse<List<DoctorResponse>>> =
        ResponseEntity.ok(ApiResponse.success(admissionService.listResidents()))

    @GetMapping("/{id}/consulting-physicians")
    @PreAuthorize("hasAuthority('admission:read')")
    fun listConsultingPhysicians(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: CustomUserDetails,
    ): ResponseEntity<ApiResponse<List<ConsultingPhysicianResponse>>> {
        val consultingPhysicians = admissionService.listConsultingPhysicians(
            id,
            resolveActiveAdmissionsOnly(currentUser),
        )
        return ResponseEntity.ok(ApiResponse.success(consultingPhysicians))
    }

    @PostMapping("/{id}/consulting-physicians")
    @PreAuthorize("hasAuthority('admission:update')")
    fun addConsultingPhysician(
        @PathVariable id: Long,
        @Valid @RequestBody request: AddConsultingPhysicianRequest,
    ): ResponseEntity<ApiResponse<ConsultingPhysicianResponse>> {
        val consultingPhysician = admissionService.addConsultingPhysician(id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(consultingPhysician))
    }

    @DeleteMapping("/{id}/consulting-physicians/{consultingPhysicianId}")
    @PreAuthorize("hasAuthority('admission:update')")
    fun removeConsultingPhysician(
        @PathVariable id: Long,
        @PathVariable consultingPhysicianId: Long,
    ): ResponseEntity<Void> {
        admissionService.removeConsultingPhysician(id, consultingPhysicianId)
        return ResponseEntity.noContent().build()
    }

    // Document endpoints

    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('admission:view-documents')")
    fun listDocuments(@PathVariable id: Long): ResponseEntity<ApiResponse<List<AdmissionDocumentResponse>>> {
        val documents = admissionDocumentService.listDocuments(id)
        return ResponseEntity.ok(ApiResponse.success(documents))
    }

    @PostMapping("/{id}/documents")
    @PreAuthorize("hasAuthority('admission:upload-documents')")
    fun uploadDocument(
        @PathVariable id: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("documentTypeId") documentTypeId: Long,
        @RequestParam(value = "displayName", required = false) displayName: String?,
    ): ResponseEntity<ApiResponse<AdmissionDocumentResponse>> {
        val document = admissionDocumentService.uploadDocument(id, documentTypeId, displayName, file)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document))
    }

    @GetMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('admission:view-documents')")
    fun getDocument(
        @PathVariable id: Long,
        @PathVariable docId: Long,
    ): ResponseEntity<ApiResponse<AdmissionDocumentResponse>> {
        val document = admissionDocumentService.getDocument(id, docId)
        return ResponseEntity.ok(ApiResponse.success(document))
    }

    @GetMapping("/{id}/documents/{docId}/file")
    @PreAuthorize("hasAuthority('admission:download-documents')")
    fun downloadDocument(@PathVariable id: Long, @PathVariable docId: Long): ResponseEntity<ByteArray> {
        val document = admissionDocumentService.downloadDocument(id, docId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .contentType(MediaType.parseMediaType(document.contentType))
            .contentLength(document.fileSize)
            .body(document.fileData)
    }

    @GetMapping("/{id}/documents/{docId}/thumbnail")
    @PreAuthorize("hasAuthority('admission:view-documents')")
    fun getThumbnail(@PathVariable id: Long, @PathVariable docId: Long): ResponseEntity<ByteArray> {
        val thumbnail = admissionDocumentService.getThumbnail(id, docId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(thumbnail.fileData)
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @PreAuthorize("hasAuthority('admission:delete-documents')")
    fun deleteDocument(@PathVariable id: Long, @PathVariable docId: Long): ResponseEntity<ApiResponse<Unit>> {
        admissionDocumentService.deleteDocument(id, docId)
        return ResponseEntity.ok(ApiResponse.success(messageService.admissionDocumentDeleted()))
    }
}

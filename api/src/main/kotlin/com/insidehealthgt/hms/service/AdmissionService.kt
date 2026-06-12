package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.AddConsultingPhysicianRequest
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
import com.insidehealthgt.hms.dto.request.DischargeAdmissionRequest
import com.insidehealthgt.hms.dto.request.UpdateAdmissionRequest
import com.insidehealthgt.hms.dto.response.AdmissionDetailResponse
import com.insidehealthgt.hms.dto.response.AdmissionListResponse
import com.insidehealthgt.hms.dto.response.ConsultingPhysicianResponse
import com.insidehealthgt.hms.dto.response.DoctorResponse
import com.insidehealthgt.hms.dto.response.PatientSummaryResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.AdmissionConsentDocument
import com.insidehealthgt.hms.entity.AdmissionConsultingPhysician
import com.insidehealthgt.hms.entity.AdmissionStatus
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.event.AdmissionCreatedEvent
import com.insidehealthgt.hms.event.PatientDischargedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionConsentDocumentRepository
import com.insidehealthgt.hms.repository.AdmissionConsultingPhysicianRepository
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.RoomRepository
import com.insidehealthgt.hms.repository.TriageCodeRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import com.insidehealthgt.hms.security.SystemRole
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions", "ThrowsCount")
class AdmissionService(
    private val admissionRepository: AdmissionRepository,
    private val admissionConsentDocumentRepository: AdmissionConsentDocumentRepository,
    private val admissionConsultingPhysicianRepository: AdmissionConsultingPhysicianRepository,
    private val patientRepository: PatientRepository,
    private val triageCodeRepository: TriageCodeRepository,
    private val roomRepository: RoomRepository,
    private val userRepository: UserRepository,
    private val fileStorageService: FileStorageService,
    private val eventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
    private val patientService: PatientService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): Admission = admissionRepository.findByIdWithRelations(id)
        ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(id))

    @Transactional(readOnly = true)
    fun findAll(
        pageable: Pageable,
        status: AdmissionStatus? = null,
        type: AdmissionType? = null,
        doctorId: Long? = null,
    ): Page<AdmissionListResponse> {
        val admissions = if (doctorId != null) {
            admissionRepository.findAllByPhysicianWithRelations(doctorId, status, type, pageable)
        } else {
            when {
                status != null && type != null ->
                    admissionRepository.findAllByStatusAndTypeWithRelations(status, type, pageable)

                status != null ->
                    admissionRepository.findAllByStatusWithRelations(status, pageable)

                type != null ->
                    admissionRepository.findAllByTypeWithRelations(type, pageable)

                else ->
                    admissionRepository.findAllWithRelations(pageable)
            }
        }
        return admissions.map { AdmissionListResponse.from(it) }
    }

    /**
     * Lists every admission for a single patient, most-recent-first.
     *
     * Authorization is a two-step **gate, not a filter**:
     *  1. [patientService.assertPatientAccessible] enforces the SAME patient-level visibility as
     *     the patient detail page — 404 if the patient is unknown, 403 (AccessDenied) if a
     *     standalone doctor is not assigned to the patient or a psychologist views a patient with
     *     no active admission.
     *  2. Once access is granted, ALL of the patient's admissions are returned (every status and
     *     type). [doctorId] / [activeAdmissionsOnly] authorize patient ACCESS only — they are
     *     deliberately NOT applied as per-admission row filters (unlike the global [findAll]).
     */
    @Transactional(readOnly = true)
    fun findAdmissionsByPatient(
        patientId: Long,
        doctorId: Long?,
        activeAdmissionsOnly: Boolean,
        pageable: Pageable,
    ): Page<AdmissionListResponse> {
        patientService.assertPatientAccessible(patientId, doctorId, activeAdmissionsOnly)
        return admissionRepository.findByPatientIdWithRelations(patientId, pageable)
            .map { AdmissionListResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAdmission(id: Long, activeOnly: Boolean = false): AdmissionDetailResponse {
        val admission = findById(id)
        if (activeOnly && admission.status != AdmissionStatus.ACTIVE) {
            throw AccessDeniedException("Access denied")
        }
        return buildAdmissionDetailResponse(admission)
    }

    @Transactional
    fun createAdmission(request: CreateAdmissionRequest): AdmissionDetailResponse {
        val patient = patientRepository.findById(request.patientId)
            .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionPatientNotFound(request.patientId)) }

        // Validate that the patient doesn't already have an active admission
        if (admissionRepository.existsActiveByPatientId(patient.id!!)) {
            throw BadRequestException(messageService.errorAdmissionPatientActive())
        }

        // Validate triage code requirement based on admission type
        if (request.type.requiresTriageCode() && request.triageCodeId == null) {
            throw BadRequestException(messageService.errorAdmissionTriageRequired(request.type.toString()))
        }

        // Load triage code if provided
        val triageCode = request.triageCodeId?.let { triageCodeId ->
            triageCodeRepository.findById(triageCodeId)
                .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionTriageNotFound(triageCodeId)) }
        }

        // Validate room requirement based on admission type
        if (request.type.requiresRoom() && request.roomId == null) {
            throw BadRequestException(messageService.errorAdmissionRoomRequired(request.type.toString()))
        }

        // Load room if provided
        val room = request.roomId?.let { roomId ->
            roomRepository.findById(roomId)
                .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionRoomNotFound(roomId)) }
        }

        val treatingPhysician = userRepository.findById(request.treatingPhysicianId)
            .orElseThrow {
                ResourceNotFoundException(messageService.errorAdmissionUserNotFound(request.treatingPhysicianId))
            }

        // Validate that the user is a doctor
        if (!treatingPhysician.hasRole(SystemRole.MEDICO)) {
            throw BadRequestException(messageService.errorAdmissionPhysicianRole())
        }

        // Residents auto-bind to themselves; admins must pick the resident.
        val resident = resolveResident(request)

        // Validate room availability if room is provided
        room?.let {
            val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(it.id!!)
            if (activeAdmissions >= it.capacity) {
                throw BadRequestException(messageService.errorAdmissionRoomFull(it.number))
            }
        }

        val admission = Admission(
            patient = patient,
            triageCode = triageCode,
            room = room,
            treatingPhysician = treatingPhysician,
            resident = resident,
            admissionDate = request.admissionDate,
            inventory = request.inventory,
            status = AdmissionStatus.ACTIVE,
            type = request.type,
        )

        val savedAdmission = admissionRepository.save(admission)

        // Publish event for procedure admission types (billing)
        if (request.type in setOf(AdmissionType.ELECTROSHOCK_THERAPY, AdmissionType.KETAMINE_INFUSION)) {
            eventPublisher.publishEvent(
                AdmissionCreatedEvent(
                    admissionId = savedAdmission.id!!,
                    admissionType = request.type,
                ),
            )
        }

        return buildAdmissionDetailResponse(savedAdmission)
    }

    @Transactional
    fun updateAdmission(id: Long, request: UpdateAdmissionRequest): AdmissionDetailResponse {
        val admission = findById(id)

        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionUpdateDischarged())
        }

        // Determine the effective type (update if provided, otherwise keep current)
        val effectiveType = request.type ?: admission.type

        // Validate triage code requirement based on admission type
        if (effectiveType.requiresTriageCode() && request.triageCodeId == null) {
            throw BadRequestException(messageService.errorAdmissionTriageRequired(effectiveType.toString()))
        }

        // Load triage code if provided
        val triageCode = request.triageCodeId?.let { triageCodeId ->
            triageCodeRepository.findById(triageCodeId)
                .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionTriageNotFound(triageCodeId)) }
        }

        // Validate room requirement based on admission type
        if (effectiveType.requiresRoom() && request.roomId == null) {
            throw BadRequestException(messageService.errorAdmissionRoomRequired(effectiveType.toString()))
        }

        // Load room if provided
        val room = request.roomId?.let { roomId ->
            roomRepository.findById(roomId)
                .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionRoomNotFound(roomId)) }
        }

        val treatingPhysician = userRepository.findById(request.treatingPhysicianId)
            .orElseThrow {
                ResourceNotFoundException(messageService.errorAdmissionUserNotFound(request.treatingPhysicianId))
            }

        // Validate that the user is a doctor
        if (!treatingPhysician.hasRole(SystemRole.MEDICO)) {
            throw BadRequestException(messageService.errorAdmissionPhysicianRole())
        }

        // Validate room availability if changing rooms
        if (room != null && room.id != admission.room?.id) {
            val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(room.id!!)
            if (activeAdmissions >= room.capacity) {
                throw BadRequestException(messageService.errorAdmissionRoomFull(room.number))
            }
        }

        admission.triageCode = triageCode
        admission.room = room
        admission.treatingPhysician = treatingPhysician
        admission.inventory = request.inventory
        request.type?.let { admission.type = it }

        val savedAdmission = admissionRepository.save(admission)
        return buildAdmissionDetailResponse(savedAdmission)
    }

    @Transactional
    fun dischargePatient(id: Long, request: DischargeAdmissionRequest): AdmissionDetailResponse {
        val admission = findById(id)

        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionAlreadyDischarged())
        }

        // A discharge comment is mandatory for everyone permitted to discharge
        // (ADMINISTRADOR and MEDICO_RESIDENTE — see admission:discharge grants).
        if (request.dischargeNote.isNullOrBlank()) {
            throw BadRequestException(messageService.errorAdmissionDischargeNoteRequired())
        }

        admission.status = AdmissionStatus.DISCHARGED
        admission.dischargeDate = LocalDateTime.now()
        admission.dischargeNote = request.dischargeNote.trim()

        val savedAdmission = admissionRepository.save(admission)

        eventPublisher.publishEvent(
            PatientDischargedEvent(
                admissionId = savedAdmission.id!!,
                patientId = savedAdmission.patient.id!!,
            ),
        )

        return buildAdmissionDetailResponse(savedAdmission)
    }

    @Transactional
    fun deleteAdmission(id: Long) {
        val admission = findById(id)
        admission.deletedAt = LocalDateTime.now()
        admissionRepository.save(admission)
    }

    @Transactional
    fun uploadConsentDocument(admissionId: Long, file: MultipartFile): AdmissionDetailResponse {
        val admission = findById(admissionId)

        validateAdmissionActive(admission)
        validateConsentFile(file)

        // Get patient ID for directory organization (consent files stored under patient directory)
        val patientId = admission.patient.id!!

        // Store file on disk FIRST (before DB transaction commits)
        val storagePath = fileStorageService.storeFile(
            patientId = patientId,
            documentType = StorageDocumentType.CONSENT_DOCUMENT,
            file = file,
        )

        // Remove existing consent document if any (soft delete - file kept on disk)
        admission.consentDocument?.let {
            it.deletedAt = LocalDateTime.now()
        }

        // Create new consent document with storage path
        val consentDocument = AdmissionConsentDocument(
            fileName = file.originalFilename ?: "consent_document",
            contentType = file.contentType ?: "application/octet-stream",
            fileSize = file.size,
            storagePath = storagePath,
            admission = admission,
        )

        admission.consentDocument = consentDocument
        val savedAdmission = admissionRepository.save(admission)
        return buildAdmissionDetailResponse(savedAdmission)
    }

    @Transactional(readOnly = true)
    fun getConsentDocument(admissionId: Long): DocumentFileData {
        val document = admissionConsentDocumentRepository.findByAdmissionId(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionConsentNotFound(admissionId))

        val fileData = fileStorageService.loadFile(document.storagePath)

        return DocumentFileData(
            fileName = document.fileName,
            contentType = document.contentType,
            fileSize = document.fileSize,
            fileData = fileData,
        )
    }

    @Transactional(readOnly = true)
    fun searchPatients(query: String, activeAdmissionsOnly: Boolean = false): List<PatientSummaryResponse> {
        if (query.isBlank()) return emptyList()

        val pageable = org.springframework.data.domain.PageRequest.of(0, PATIENT_SEARCH_LIMIT)
        val patients = if (activeAdmissionsOnly) {
            patientRepository.searchByNameOrDocumentWithActiveAdmission(query.trim(), pageable)
        } else {
            patientRepository.searchByNameOrDocument(query.trim(), pageable)
        }
        return patients.content.map { patient ->
            val hasActiveAdmission = admissionRepository.existsActiveByPatientId(patient.id!!)
            PatientSummaryResponse.from(patient, hasActiveAdmission = hasActiveAdmission)
        }
    }

    @Transactional(readOnly = true)
    fun getPatientSummary(patientId: Long, activeAdmissionsOnly: Boolean = false): PatientSummaryResponse {
        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionPatientNotFound(patientId)) }
        val hasActiveAdmission = admissionRepository.existsActiveByPatientId(patient.id!!)
        if (activeAdmissionsOnly && !hasActiveAdmission) {
            throw AccessDeniedException("Access denied")
        }
        return PatientSummaryResponse.from(patient, hasActiveAdmission = hasActiveAdmission)
    }

    companion object {
        private const val PATIENT_SEARCH_LIMIT = 20
    }

    @Transactional(readOnly = true)
    fun listDoctors(): List<DoctorResponse> {
        val doctors = userRepository.findByRoleCode(SystemRole.MEDICO)
        return doctors.map { DoctorResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun listResidents(): List<DoctorResponse> {
        val residents = userRepository.findByRoleCode(SystemRole.MEDICO_RESIDENTE)
        return residents.map { DoctorResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun listConsultingPhysicians(admissionId: Long, activeOnly: Boolean = false): List<ConsultingPhysicianResponse> {
        // Verify admission exists and check active status
        val admission = findById(admissionId)
        if (activeOnly && admission.status != AdmissionStatus.ACTIVE) {
            throw AccessDeniedException("Access denied")
        }

        val consultingPhysicians = admissionConsultingPhysicianRepository.findByAdmissionIdWithPhysician(admissionId)

        // Batch fetch createdBy users to avoid N+1 queries
        val createdByIds = consultingPhysicians.mapNotNull { it.createdBy }.distinct()
        val createdByUsers = if (createdByIds.isNotEmpty()) {
            userRepository.findAllById(createdByIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return consultingPhysicians.map { cp ->
            val createdByUser = cp.createdBy?.let { createdByUsers[it] }
            ConsultingPhysicianResponse.from(cp, createdByUser)
        }
    }

    @Transactional
    fun addConsultingPhysician(admissionId: Long, request: AddConsultingPhysicianRequest): ConsultingPhysicianResponse {
        val admission = findById(admissionId)

        validateAdmissionActive(admission)

        val physician = userRepository.findById(request.physicianId)
            .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionUserNotFound(request.physicianId)) }

        // Validate that the user is a doctor
        if (!physician.hasRole(SystemRole.MEDICO)) {
            throw BadRequestException(messageService.errorAdmissionConsultingPhysicianRole())
        }

        // Validate that the physician is not the treating physician
        if (physician.id == admission.treatingPhysician.id) {
            throw BadRequestException(messageService.errorAdmissionConsultingPhysicianIsTreating())
        }

        // Check if the physician is already assigned
        val alreadyAssigned = admissionConsultingPhysicianRepository
            .existsByAdmissionIdAndPhysicianIdAndNotDeleted(admissionId, physician.id!!)
        if (alreadyAssigned) {
            throw BadRequestException(messageService.errorAdmissionConsultingPhysicianAlreadyAssigned())
        }

        val consultingPhysician = AdmissionConsultingPhysician(
            admission = admission,
            physician = physician,
            reason = request.reason,
            requestedDate = request.requestedDate,
        )

        val saved = admissionConsultingPhysicianRepository.save(consultingPhysician)
        val createdByUser = saved.createdBy?.let { userRepository.findById(it).orElse(null) }
        return ConsultingPhysicianResponse.from(saved, createdByUser)
    }

    @Transactional
    fun removeConsultingPhysician(admissionId: Long, consultingPhysicianId: Long) {
        // Verify admission exists and is not discharged (record immutable post-discharge)
        validateAdmissionActive(findById(admissionId))

        val consultingPhysician = admissionConsultingPhysicianRepository
            .findByIdAndAdmissionId(consultingPhysicianId, admissionId)
            ?: throw ResourceNotFoundException(
                messageService.errorAdmissionConsultingPhysicianNotFound(consultingPhysicianId, admissionId),
            )

        // Soft delete
        consultingPhysician.deletedAt = LocalDateTime.now()
        admissionConsultingPhysicianRepository.save(consultingPhysician)
    }

    private fun resolveResident(request: CreateAdmissionRequest): User {
        val principal = currentUserProvider.currentUserDetails()
            ?: throw BadRequestException(messageService.errorAdmissionResidentRoleRequired())

        // ADMINISTRADOR is the only exception: admins are not residents, so they must
        // explicitly pick the resident doctor the admission is recorded under.
        if (principal.hasRole(SystemRole.ADMINISTRADOR)) {
            val residentId = request.residentId
                ?: throw BadRequestException(messageService.errorAdmissionResidentRequired())
            val resident = userRepository.findById(residentId)
                .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionUserNotFound(residentId)) }
            if (!resident.hasRole(SystemRole.MEDICO_RESIDENTE)) {
                throw BadRequestException(messageService.errorAdmissionResidentInvalidRole())
            }
            return resident
        }

        // Everyone else must be a resident; the resident is always themselves.
        if (!principal.hasRole(SystemRole.MEDICO_RESIDENTE)) {
            throw BadRequestException(messageService.errorAdmissionResidentRoleRequired())
        }
        return userRepository.findById(principal.id)
            .orElseThrow { ResourceNotFoundException(messageService.errorAdmissionUserNotFound(principal.id)) }
    }

    /**
     * Discharge protection: once an admission is discharged its record is immutable —
     * no consent uploads and no consulting-physician changes. Admission field edits and
     * discharge have their own dedicated guards ([updateAdmission] / [dischargePatient]).
     */
    private fun validateAdmissionActive(admission: Admission) {
        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionDischargedRecords())
        }
    }

    private fun validateConsentFile(file: MultipartFile) {
        val error = when {
            file.isEmpty -> messageService.errorAdmissionConsentFileEmpty()

            file.size > AdmissionConsentDocument.MAX_FILE_SIZE ->
                messageService.errorAdmissionConsentFileSize()

            file.contentType == null || file.contentType !in AdmissionConsentDocument.ALLOWED_CONTENT_TYPES ->
                messageService.errorAdmissionConsentFileType()

            else -> null
        }
        error?.let { throw BadRequestException(it) }
    }

    private fun buildAdmissionDetailResponse(admission: Admission): AdmissionDetailResponse {
        val createdByUser = admission.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = admission.updatedBy?.let { userRepository.findById(it).orElse(null) }

        // Fetch createdBy users for consulting physicians
        val cpCreatedByIds = admission.consultingPhysicians.mapNotNull { it.createdBy }.distinct()
        val cpCreatedByUsers = if (cpCreatedByIds.isNotEmpty()) {
            userRepository.findAllById(cpCreatedByIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return AdmissionDetailResponse.from(admission, createdByUser, updatedByUser, cpCreatedByUsers)
    }
}

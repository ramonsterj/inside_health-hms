package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.AddConsultingPhysicianRequest
import com.insidehealthgt.hms.dto.request.CreateAdmissionRequest
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): Admission = admissionRepository.findByIdWithRelations(id)
        ?: throw ResourceNotFoundException("Admission not found with id: $id")

    @Transactional(readOnly = true)
    fun findAll(
        pageable: Pageable,
        status: AdmissionStatus? = null,
        type: AdmissionType? = null,
    ): Page<AdmissionListResponse> {
        val admissions = when {
            status != null && type != null ->
                admissionRepository.findAllByStatusAndTypeWithRelations(status, type, pageable)

            status != null ->
                admissionRepository.findAllByStatusWithRelations(status, pageable)

            type != null ->
                admissionRepository.findAllByTypeWithRelations(type, pageable)

            else ->
                admissionRepository.findAllWithRelations(pageable)
        }
        return admissions.map { AdmissionListResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAdmission(id: Long): AdmissionDetailResponse {
        val admission = findById(id)
        return buildAdmissionDetailResponse(admission)
    }

    @Transactional
    fun createAdmission(request: CreateAdmissionRequest): AdmissionDetailResponse {
        val patient = patientRepository.findById(request.patientId)
            .orElseThrow { ResourceNotFoundException("Patient not found with id: ${request.patientId}") }

        // Validate that the patient doesn't already have an active admission
        if (admissionRepository.existsActiveByPatientId(patient.id!!)) {
            throw BadRequestException("Patient already has an active admission")
        }

        // Validate triage code requirement based on admission type
        if (request.type.requiresTriageCode() && request.triageCodeId == null) {
            throw BadRequestException("Triage code is required for ${request.type} admissions")
        }

        // Load triage code if provided
        val triageCode = request.triageCodeId?.let { triageCodeId ->
            triageCodeRepository.findById(triageCodeId)
                .orElseThrow { ResourceNotFoundException("Triage code not found with id: $triageCodeId") }
        }

        // Validate room requirement based on admission type
        if (request.type.requiresRoom() && request.roomId == null) {
            throw BadRequestException("Room is required for ${request.type} admissions")
        }

        // Load room if provided
        val room = request.roomId?.let { roomId ->
            roomRepository.findById(roomId)
                .orElseThrow { ResourceNotFoundException("Room not found with id: $roomId") }
        }

        val treatingPhysician = userRepository.findById(request.treatingPhysicianId)
            .orElseThrow { ResourceNotFoundException("User not found with id: ${request.treatingPhysicianId}") }

        // Validate that the user is a doctor
        if (!treatingPhysician.hasRole("DOCTOR")) {
            throw BadRequestException("Treating physician must have the DOCTOR role")
        }

        // Validate room availability if room is provided
        room?.let {
            val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(it.id!!)
            if (activeAdmissions >= it.capacity) {
                throw BadRequestException("Room '${it.number}' is full. No available beds.")
            }
        }

        val admission = Admission(
            patient = patient,
            triageCode = triageCode,
            room = room,
            treatingPhysician = treatingPhysician,
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
            throw BadRequestException("Cannot update a discharged admission")
        }

        // Determine the effective type (update if provided, otherwise keep current)
        val effectiveType = request.type ?: admission.type

        // Validate triage code requirement based on admission type
        if (effectiveType.requiresTriageCode() && request.triageCodeId == null) {
            throw BadRequestException("Triage code is required for $effectiveType admissions")
        }

        // Load triage code if provided
        val triageCode = request.triageCodeId?.let { triageCodeId ->
            triageCodeRepository.findById(triageCodeId)
                .orElseThrow { ResourceNotFoundException("Triage code not found with id: $triageCodeId") }
        }

        // Validate room requirement based on admission type
        if (effectiveType.requiresRoom() && request.roomId == null) {
            throw BadRequestException("Room is required for $effectiveType admissions")
        }

        // Load room if provided
        val room = request.roomId?.let { roomId ->
            roomRepository.findById(roomId)
                .orElseThrow { ResourceNotFoundException("Room not found with id: $roomId") }
        }

        val treatingPhysician = userRepository.findById(request.treatingPhysicianId)
            .orElseThrow { ResourceNotFoundException("User not found with id: ${request.treatingPhysicianId}") }

        // Validate that the user is a doctor
        if (!treatingPhysician.hasRole("DOCTOR")) {
            throw BadRequestException("Treating physician must have the DOCTOR role")
        }

        // Validate room availability if changing rooms
        if (room != null && room.id != admission.room?.id) {
            val activeAdmissions = roomRepository.countActiveAdmissionsByRoomId(room.id!!)
            if (activeAdmissions >= room.capacity) {
                throw BadRequestException("Room '${room.number}' is full. No available beds.")
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
    fun dischargePatient(id: Long): AdmissionDetailResponse {
        val admission = findById(id)

        if (admission.isDischarged()) {
            throw BadRequestException("Patient is already discharged")
        }

        admission.status = AdmissionStatus.DISCHARGED
        admission.dischargeDate = LocalDateTime.now()

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
            ?: throw ResourceNotFoundException("Consent document not found for admission: $admissionId")

        val fileData = fileStorageService.loadFile(document.storagePath)

        return DocumentFileData(
            fileName = document.fileName,
            contentType = document.contentType,
            fileSize = document.fileSize,
            fileData = fileData,
        )
    }

    @Transactional(readOnly = true)
    fun searchPatients(query: String): List<PatientSummaryResponse> {
        if (query.isBlank()) return emptyList()

        val patients = patientRepository.searchByNameOrDocument(
            query.trim(),
            org.springframework.data.domain.PageRequest.of(0, PATIENT_SEARCH_LIMIT),
        )
        return patients.content.map { patient ->
            val hasActiveAdmission = admissionRepository.existsActiveByPatientId(patient.id!!)
            PatientSummaryResponse.from(patient, hasActiveAdmission = hasActiveAdmission)
        }
    }

    @Transactional(readOnly = true)
    fun getPatientSummary(patientId: Long): PatientSummaryResponse {
        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResourceNotFoundException("Patient not found with id: $patientId") }
        val hasActiveAdmission = admissionRepository.existsActiveByPatientId(patient.id!!)
        return PatientSummaryResponse.from(patient, hasActiveAdmission = hasActiveAdmission)
    }

    companion object {
        private const val PATIENT_SEARCH_LIMIT = 20
    }

    @Transactional(readOnly = true)
    fun listDoctors(): List<DoctorResponse> {
        val doctors = userRepository.findByRoleCode("DOCTOR")
        return doctors.map { DoctorResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun listConsultingPhysicians(admissionId: Long): List<ConsultingPhysicianResponse> {
        // Verify admission exists
        findById(admissionId)

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

        val physician = userRepository.findById(request.physicianId)
            .orElseThrow { ResourceNotFoundException("User not found with id: ${request.physicianId}") }

        // Validate that the user is a doctor
        if (!physician.hasRole("DOCTOR")) {
            throw BadRequestException("Consulting physician must have the DOCTOR role")
        }

        // Validate that the physician is not the treating physician
        if (physician.id == admission.treatingPhysician.id) {
            throw BadRequestException("Cannot add treating physician as a consulting physician")
        }

        // Check if the physician is already assigned
        val alreadyAssigned = admissionConsultingPhysicianRepository
            .existsByAdmissionIdAndPhysicianIdAndNotDeleted(admissionId, physician.id!!)
        if (alreadyAssigned) {
            throw BadRequestException("Physician is already assigned as a consultant for this admission")
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
        // Verify admission exists
        findById(admissionId)

        val consultingPhysician = admissionConsultingPhysicianRepository
            .findByIdAndAdmissionId(consultingPhysicianId, admissionId)
            ?: throw ResourceNotFoundException(
                "Consulting physician not found with id: $consultingPhysicianId for admission: $admissionId",
            )

        // Soft delete
        consultingPhysician.deletedAt = LocalDateTime.now()
        admissionConsultingPhysicianRepository.save(consultingPhysician)
    }

    private fun validateConsentFile(file: MultipartFile) {
        val error = when {
            file.isEmpty -> "File is empty"

            file.size > AdmissionConsentDocument.MAX_FILE_SIZE ->
                "File size exceeds maximum allowed size of 25MB"

            file.contentType == null || file.contentType !in AdmissionConsentDocument.ALLOWED_CONTENT_TYPES ->
                "Invalid file type. Allowed types: JPEG, PNG, PDF"

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

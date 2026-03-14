package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreatePatientRequest
import com.insidehealthgt.hms.dto.request.UpdatePatientRequest
import com.insidehealthgt.hms.dto.response.PatientResponse
import com.insidehealthgt.hms.dto.response.PatientSummaryResponse
import com.insidehealthgt.hms.entity.EmergencyContact
import com.insidehealthgt.hms.entity.Patient
import com.insidehealthgt.hms.entity.PatientIdDocument
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.DuplicatePatientException
import com.insidehealthgt.hms.exception.DuplicatePatientInfo
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PatientIdDocumentRepository
import com.insidehealthgt.hms.repository.PatientRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class PatientService(
    private val patientRepository: PatientRepository,
    private val patientIdDocumentRepository: PatientIdDocumentRepository,
    private val userRepository: UserRepository,
    private val admissionRepository: AdmissionRepository,
    private val fileStorageService: FileStorageService,
    private val messageService: MessageService,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): Patient = patientRepository.findById(id)
        .orElseThrow { ResourceNotFoundException(messageService.errorPatientNotFound(id)) }

    @Transactional(readOnly = true)
    fun findByIdWithContacts(id: Long): Patient = patientRepository.findByIdWithContacts(id)
        ?: throw ResourceNotFoundException(messageService.errorPatientNotFound(id))

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable, search: String? = null, doctorId: Long? = null): Page<PatientSummaryResponse> {
        val sanitizedSearch = search?.takeIf { it.isNotBlank() }
            ?.trim()?.replace("\\", "\\\\")?.replace("%", "\\%")?.replace("_", "\\_")

        val patients: Page<Patient> = if (doctorId != null) {
            if (sanitizedSearch != null) {
                patientRepository.searchByNameOrDocumentForPhysician(sanitizedSearch, doctorId, pageable)
            } else {
                patientRepository.findAllByPhysician(doctorId, pageable)
            }
        } else if (sanitizedSearch != null) {
            patientRepository.searchByNameOrDocument(sanitizedSearch, pageable)
        } else {
            patientRepository.findAll(pageable)
        }

        // Batch-fetch which patients have ID documents and active admissions
        val patientIds: List<Long> = patients.content.mapNotNull { it.id }
        val patientsWithIdDocument: Set<Long> = if (patientIds.isNotEmpty()) {
            patientRepository.findPatientIdsWithIdDocument(patientIds).toSet()
        } else {
            emptySet()
        }
        val patientsWithActiveAdmission: Set<Long> = if (patientIds.isNotEmpty()) {
            patientRepository.findPatientIdsWithActiveAdmission(patientIds).toSet()
        } else {
            emptySet()
        }

        return patients.map { patient ->
            val hasIdDoc = patient.id?.let { it in patientsWithIdDocument } ?: false
            val hasActiveAdmission = patient.id?.let { it in patientsWithActiveAdmission } ?: false
            PatientSummaryResponse.from(patient, hasIdDoc, hasActiveAdmission)
        }
    }

    @Transactional(readOnly = true)
    fun getPatient(id: Long, doctorId: Long? = null): PatientResponse {
        val patient = findByIdWithContacts(id)
        if (doctorId != null && !patientRepository.isPatientAssignedToDoctor(id, doctorId)) {
            throw AccessDeniedException(messageService.errorPatientAccessDenied())
        }
        val hasActiveAdmission = admissionRepository.existsActiveByPatientId(id)
        return buildPatientResponse(patient, hasActiveAdmission)
    }

    @Transactional
    fun createPatient(request: CreatePatientRequest): PatientResponse {
        // Check for potential duplicates
        val duplicates = patientRepository.findPotentialDuplicates(
            firstName = request.firstName,
            lastName = request.lastName,
            age = request.age,
            idDocumentNumber = request.idDocumentNumber,
        )

        if (duplicates.isNotEmpty()) {
            throw DuplicatePatientException(
                message = messageService.errorPatientDuplicateFound(),
                potentialDuplicates = duplicates.map { p: Patient ->
                    DuplicatePatientInfo(
                        id = p.id!!,
                        firstName = p.firstName,
                        lastName = p.lastName,
                        age = p.age,
                        idDocumentNumber = p.idDocumentNumber,
                    )
                },
            )
        }

        val patient = Patient(
            firstName = request.firstName,
            lastName = request.lastName,
            age = request.age,
            sex = request.sex,
            gender = request.gender,
            maritalStatus = request.maritalStatus,
            religion = request.religion,
            educationLevel = request.educationLevel,
            occupation = request.occupation,
            address = request.address,
            email = request.email,
            idDocumentNumber = request.idDocumentNumber,
            notes = request.notes,
        )

        // Add emergency contacts
        request.emergencyContacts.forEach { contactRequest ->
            val contact = EmergencyContact(
                name = contactRequest.name,
                relationship = contactRequest.relationship,
                phone = contactRequest.phone,
            )
            patient.addEmergencyContact(contact)
        }

        val savedPatient = patientRepository.save(patient)
        return buildPatientResponse(savedPatient)
    }

    @Transactional
    fun updatePatient(id: Long, request: UpdatePatientRequest): PatientResponse {
        val patient = findByIdWithContacts(id)

        // Check for duplicates (excluding the current patient)
        val duplicates = patientRepository.findPotentialDuplicates(
            firstName = request.firstName,
            lastName = request.lastName,
            age = request.age,
            idDocumentNumber = request.idDocumentNumber,
        ).filter { it.id != id }

        if (duplicates.isNotEmpty()) {
            throw DuplicatePatientException(
                message = messageService.errorPatientDuplicateFound(),
                potentialDuplicates = duplicates.map { p: Patient ->
                    DuplicatePatientInfo(
                        id = p.id!!,
                        firstName = p.firstName,
                        lastName = p.lastName,
                        age = p.age,
                        idDocumentNumber = p.idDocumentNumber,
                    )
                },
            )
        }

        // Update basic fields
        patient.firstName = request.firstName
        patient.lastName = request.lastName
        patient.age = request.age
        patient.sex = request.sex
        patient.gender = request.gender
        patient.maritalStatus = request.maritalStatus
        patient.religion = request.religion
        patient.educationLevel = request.educationLevel
        patient.occupation = request.occupation
        patient.address = request.address
        patient.email = request.email
        patient.idDocumentNumber = request.idDocumentNumber
        patient.notes = request.notes

        // Update emergency contacts
        updateEmergencyContacts(patient, request)

        val savedPatient = patientRepository.save(patient)
        return buildPatientResponse(savedPatient)
    }

    @Transactional
    fun uploadIdDocument(patientId: Long, file: MultipartFile): PatientResponse {
        val patient = findByIdWithContacts(patientId)

        // Validate file
        validateFile(file)

        // Store file on disk FIRST (before DB transaction commits)
        val storagePath = fileStorageService.storeFile(
            patientId = patientId,
            documentType = StorageDocumentType.ID_DOCUMENT,
            file = file,
        )

        // Remove existing document if any (soft delete - file kept on disk)
        patient.idDocument?.let {
            it.deletedAt = LocalDateTime.now()
        }

        // Create new document with storage path
        val document = PatientIdDocument(
            fileName = file.originalFilename ?: "document",
            contentType = file.contentType ?: "application/octet-stream",
            fileSize = file.size,
            storagePath = storagePath,
            patient = patient,
        )

        patient.idDocument = document
        val savedPatient = patientRepository.save(patient)
        return buildPatientResponse(savedPatient)
    }

    @Transactional(readOnly = true)
    fun getIdDocument(patientId: Long): DocumentFileData {
        val document = patientIdDocumentRepository.findByPatientId(patientId)
            ?: throw ResourceNotFoundException(messageService.errorPatientIdDocumentNotFound(patientId))

        val fileData = fileStorageService.loadFile(document.storagePath)

        return DocumentFileData(
            fileName = document.fileName,
            contentType = document.contentType,
            fileSize = document.fileSize,
            fileData = fileData,
        )
    }

    @Transactional
    fun deleteIdDocument(patientId: Long): PatientResponse {
        val patient = findByIdWithContacts(patientId)

        patient.idDocument?.let {
            it.deletedAt = LocalDateTime.now()
            patient.idDocument = null
        } ?: throw ResourceNotFoundException(messageService.errorPatientIdDocumentNotFound(patientId))

        val savedPatient = patientRepository.save(patient)
        return buildPatientResponse(savedPatient)
    }

    private fun validateFile(file: MultipartFile) {
        val error = when {
            file.isEmpty -> messageService.errorPatientFileEmpty()

            file.size > PatientIdDocument.MAX_FILE_SIZE -> messageService.errorPatientFileSize()

            file.contentType == null || file.contentType !in PatientIdDocument.ALLOWED_CONTENT_TYPES ->
                messageService.errorPatientFileType()

            else -> null
        }
        error?.let { throw BadRequestException(it) }
    }

    private fun updateEmergencyContacts(patient: Patient, request: UpdatePatientRequest) {
        // Get existing contact IDs
        val existingContactIds = patient.emergencyContacts.map { it.id }.toSet()

        // Get request contact IDs (for updates)
        val requestContactIds = request.emergencyContacts.mapNotNull { it.id }.toSet()

        // Remove contacts that are not in the request
        val contactsToRemove = patient.emergencyContacts.filter { it.id !in requestContactIds }
        contactsToRemove.forEach { contact ->
            contact.deletedAt = LocalDateTime.now()
        }
        patient.emergencyContacts.removeAll(contactsToRemove)

        // Update existing contacts and add new ones
        request.emergencyContacts.forEach { contactRequest ->
            if (contactRequest.id != null && contactRequest.id in existingContactIds) {
                // Update existing contact
                val contact = patient.emergencyContacts.find { it.id == contactRequest.id }
                contact?.apply {
                    name = contactRequest.name
                    relationship = contactRequest.relationship
                    phone = contactRequest.phone
                }
            } else {
                // Add new contact
                val newContact = EmergencyContact(
                    name = contactRequest.name,
                    relationship = contactRequest.relationship,
                    phone = contactRequest.phone,
                )
                patient.addEmergencyContact(newContact)
            }
        }
    }

    @Transactional
    fun deletePatient(id: Long) {
        val patient = findById(id)
        if (admissionRepository.existsActiveByPatientId(id)) {
            throw BadRequestException(messageService.getMessage("error.patient.activeAdmission"))
        }
        patient.deletedAt = LocalDateTime.now()
        patientRepository.save(patient)
    }

    private fun buildPatientResponse(patient: Patient, hasActiveAdmission: Boolean = false): PatientResponse {
        val createdByUser = patient.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = patient.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return PatientResponse.from(patient, createdByUser, updatedByUser, hasActiveAdmission)
    }
}

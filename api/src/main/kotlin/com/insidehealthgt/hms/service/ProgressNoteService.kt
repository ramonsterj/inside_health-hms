package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateProgressNoteRequest
import com.insidehealthgt.hms.dto.response.ProgressNoteResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.ProgressNote
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Edit policy: only ADMIN can update existing progress notes; doctors, nurses, and
 * chief nurses are append-only. The `@PreAuthorize("hasAuthority('progress-note:update')")`
 * on the controller is the first gate (only ADMIN holds the permission after V096).
 * `assertAdmin()` here is intentional defense-in-depth so the rule continues to hold
 * if the permission is later widened. Discharge protection blocks all writes — including
 * for ADMIN — and is enforced unconditionally.
 */
@Service
class ProgressNoteService(
    private val progressNoteRepository: ProgressNoteRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @Transactional(readOnly = true)
    fun listProgressNotes(admissionId: Long, pageable: Pageable): Page<ProgressNoteResponse> {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val notes = progressNoteRepository.findByAdmissionIdWithRelations(admissionId, pageable)

        val userIds = notes.content.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return notes.map { note ->
            ProgressNoteResponse.from(
                progressNote = note,
                createdByUser = note.createdBy?.let { users[it] },
                updatedByUser = note.updatedBy?.let { users[it] },
                canEdit = computeCanEdit(currentUser, admission.isActive()),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getProgressNote(admissionId: Long, noteId: Long): ProgressNoteResponse {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val note = progressNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException("Progress note not found with id: $noteId for admission: $admissionId")

        return buildResponse(note, currentUser, admission.isActive())
    }

    @Transactional
    fun createProgressNote(admissionId: Long, request: CreateProgressNoteRequest): ProgressNoteResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        validateAdmissionActive(admission)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val note = ProgressNote(
            admission = admission,
            subjectiveData = request.subjectiveData,
            objectiveData = request.objectiveData,
            analysis = request.analysis,
            actionPlans = request.actionPlans,
        )

        val saved = progressNoteRepository.save(note)
        return buildResponse(saved, currentUser, admission.isActive())
    }

    @Transactional
    fun updateProgressNote(admissionId: Long, noteId: Long, request: UpdateProgressNoteRequest): ProgressNoteResponse {
        val admission = getAdmissionOrThrow(admissionId)
        validateAdmissionActive(admission)

        val note = progressNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException("Progress note not found with id: $noteId for admission: $admissionId")

        val currentUser = currentUserProvider.currentUserDetailsOrThrow()
        assertAdmin(currentUser)

        note.subjectiveData = request.subjectiveData
        note.objectiveData = request.objectiveData
        note.analysis = request.analysis
        note.actionPlans = request.actionPlans

        val saved = progressNoteRepository.save(note)
        return buildResponse(saved, currentUser, admission.isActive())
    }

    private fun getAdmissionOrThrow(admissionId: Long): Admission =
        admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

    private fun validateAdmissionActive(admission: Admission) {
        if (admission.isDischarged()) {
            throw BadRequestException(messageService.errorAdmissionDischargedRecords())
        }
    }

    private fun assertAdmin(currentUser: CustomUserDetails) {
        if (!currentUser.hasRole("ADMIN")) {
            throw ForbiddenException(messageService.errorForbidden())
        }
    }

    private fun computeCanEdit(currentUser: CustomUserDetails, admissionActive: Boolean): Boolean =
        admissionActive && currentUser.hasRole("ADMIN")

    private fun buildResponse(
        note: ProgressNote,
        currentUser: CustomUserDetails,
        admissionActive: Boolean,
    ): ProgressNoteResponse {
        val userIds = listOfNotNull(note.createdBy, note.updatedBy).distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return ProgressNoteResponse.from(
            progressNote = note,
            createdByUser = note.createdBy?.let { users[it] },
            updatedByUser = note.updatedBy?.let { users[it] },
            canEdit = computeCanEdit(currentUser, admissionActive),
        )
    }
}

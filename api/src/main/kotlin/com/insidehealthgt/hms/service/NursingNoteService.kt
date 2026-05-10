package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateNursingNoteRequest
import com.insidehealthgt.hms.dto.response.NursingNoteResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.NursingNote
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Edit policy: only ADMIN can update existing nursing notes; doctors, nurses, and
 * chief nurses are append-only. The `@PreAuthorize("hasAuthority('nursing-note:update')")`
 * on the controller is the first gate (only ADMIN holds the permission after V096).
 * `assertAdmin()` here is intentional defense-in-depth so the rule continues to hold
 * if the permission is later widened. Discharge protection blocks all writes — including
 * for ADMIN — and is enforced unconditionally.
 *
 * Vital signs follow a different pattern (24h creator window with admin override) — see
 * VitalSignService.
 */
@Service
class NursingNoteService(
    private val nursingNoteRepository: NursingNoteRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
    private val currentUserProvider: CurrentUserProvider,
) {

    @Transactional(readOnly = true)
    fun listNursingNotes(admissionId: Long, pageable: Pageable): Page<NursingNoteResponse> {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val notes = nursingNoteRepository.findByAdmissionIdWithRelations(admissionId, pageable)

        val userIds = notes.content.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return notes.map { note ->
            NursingNoteResponse.from(
                nursingNote = note,
                createdByUser = note.createdBy?.let { users[it] },
                updatedByUser = note.updatedBy?.let { users[it] },
                canEdit = computeCanEdit(currentUser, admission.isActive()),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getNursingNote(admissionId: Long, noteId: Long): NursingNoteResponse {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val note = nursingNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException(messageService.errorNursingNoteNotFound(noteId, admissionId))

        return buildResponse(note, currentUser, admission.isActive())
    }

    @Transactional
    fun createNursingNote(admissionId: Long, request: CreateNursingNoteRequest): NursingNoteResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        validateAdmissionActive(admission)
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()

        val note = NursingNote(
            admission = admission,
            description = request.description,
        )

        val saved = nursingNoteRepository.save(note)
        return buildResponse(saved, currentUser, admission.isActive())
    }

    @Transactional
    fun updateNursingNote(admissionId: Long, noteId: Long, request: UpdateNursingNoteRequest): NursingNoteResponse {
        val admission = getAdmissionOrThrow(admissionId)
        validateAdmissionActive(admission)

        val note = nursingNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException(messageService.errorNursingNoteNotFound(noteId, admissionId))

        val currentUser = currentUserProvider.currentUserDetailsOrThrow()
        assertAdmin(currentUser)

        note.description = request.description

        val saved = nursingNoteRepository.save(note)
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
        note: NursingNote,
        currentUser: CustomUserDetails,
        admissionActive: Boolean,
    ): NursingNoteResponse {
        val userIds = listOfNotNull(note.createdBy, note.updatedBy).distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return NursingNoteResponse.from(
            nursingNote = note,
            createdByUser = note.createdBy?.let { users[it] },
            updatedByUser = note.updatedBy?.let { users[it] },
            canEdit = computeCanEdit(currentUser, admissionActive),
        )
    }
}

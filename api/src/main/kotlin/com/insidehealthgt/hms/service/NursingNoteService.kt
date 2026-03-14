package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateNursingNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateNursingNoteRequest
import com.insidehealthgt.hms.dto.response.NursingNoteResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.BaseEntity
import com.insidehealthgt.hms.entity.NursingNote
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.NursingNoteRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NursingNoteService(
    private val nursingNoteRepository: NursingNoteRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {
    companion object {
        private const val EDIT_WINDOW_HOURS = 24L
    }

    @Transactional(readOnly = true)
    fun listNursingNotes(admissionId: Long, pageable: Pageable): Page<NursingNoteResponse> {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = getCurrentUserDetails()

        val notes = nursingNoteRepository.findByAdmissionIdWithRelations(admissionId, pageable)

        // Batch fetch users to avoid N+1 queries
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
                canEdit = computeCanEdit(note, currentUser, admission.isActive()),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getNursingNote(admissionId: Long, noteId: Long): NursingNoteResponse {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = getCurrentUserDetails()

        val note = nursingNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException(messageService.errorNursingNoteNotFound(noteId, admissionId))

        return buildResponse(note, currentUser, admission.isActive())
    }

    @Transactional
    fun createNursingNote(admissionId: Long, request: CreateNursingNoteRequest): NursingNoteResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        validateAdmissionActive(admission)
        val currentUser = getCurrentUserDetails()

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

        val currentUser = getCurrentUserDetails()
        validateEditPermission(note, currentUser)

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

    @Suppress("ThrowsCount")
    private fun validateEditPermission(entity: BaseEntity, currentUser: CustomUserDetails) {
        if (currentUser.hasRole("ADMIN")) return

        if (entity.createdBy != currentUser.id) {
            throw ForbiddenException(messageService.errorEditOnlyOwnRecords())
        }

        val createdAt = entity.createdAt
            ?: throw ForbiddenException(messageService.errorEditWindowClosed())

        if (!createdAt.plusHours(EDIT_WINDOW_HOURS).isAfter(LocalDateTime.now())) {
            throw ForbiddenException(messageService.errorEditWindowClosed())
        }
    }

    @Suppress("ReturnCount")
    private fun computeCanEdit(entity: BaseEntity, currentUser: CustomUserDetails, admissionActive: Boolean): Boolean {
        if (!admissionActive) return false
        if (currentUser.hasRole("ADMIN")) return true
        if (entity.createdBy != currentUser.id) return false
        val createdAt = entity.createdAt ?: return false
        return createdAt.plusHours(EDIT_WINDOW_HOURS).isAfter(LocalDateTime.now())
    }

    private fun getCurrentUserDetails(): CustomUserDetails {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException(messageService.errorNotAuthenticated())
        return auth.principal as CustomUserDetails
    }

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
            canEdit = computeCanEdit(note, currentUser, admissionActive),
        )
    }
}

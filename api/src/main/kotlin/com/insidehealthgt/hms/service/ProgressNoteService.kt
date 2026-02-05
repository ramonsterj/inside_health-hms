package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateProgressNoteRequest
import com.insidehealthgt.hms.dto.request.UpdateProgressNoteRequest
import com.insidehealthgt.hms.dto.response.ProgressNoteResponse
import com.insidehealthgt.hms.entity.ProgressNote
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.ProgressNoteRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProgressNoteService(
    private val progressNoteRepository: ProgressNoteRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listProgressNotes(admissionId: Long, pageable: Pageable): Page<ProgressNoteResponse> {
        verifyAdmissionExists(admissionId)

        val notes = progressNoteRepository.findByAdmissionIdWithRelations(admissionId, pageable)

        // Batch fetch users to avoid N+1 queries
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
            )
        }
    }

    @Transactional(readOnly = true)
    fun getProgressNote(admissionId: Long, noteId: Long): ProgressNoteResponse {
        verifyAdmissionExists(admissionId)

        val note = progressNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException("Progress note not found with id: $noteId for admission: $admissionId")

        return buildResponse(note)
    }

    @Transactional
    fun createProgressNote(admissionId: Long, request: CreateProgressNoteRequest): ProgressNoteResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val note = ProgressNote(
            admission = admission,
            subjectiveData = request.subjectiveData,
            objectiveData = request.objectiveData,
            analysis = request.analysis,
            actionPlans = request.actionPlans,
        )

        val saved = progressNoteRepository.save(note)
        return buildResponse(saved)
    }

    @Transactional
    fun updateProgressNote(admissionId: Long, noteId: Long, request: UpdateProgressNoteRequest): ProgressNoteResponse {
        verifyAdmissionExists(admissionId)

        val note = progressNoteRepository.findByIdAndAdmissionId(noteId, admissionId)
            ?: throw ResourceNotFoundException("Progress note not found with id: $noteId for admission: $admissionId")

        note.subjectiveData = request.subjectiveData
        note.objectiveData = request.objectiveData
        note.analysis = request.analysis
        note.actionPlans = request.actionPlans

        val saved = progressNoteRepository.save(note)
        return buildResponse(saved)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun buildResponse(note: ProgressNote): ProgressNoteResponse {
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
        )
    }
}

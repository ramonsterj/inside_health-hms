package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyActivityRequest
import com.insidehealthgt.hms.dto.response.PsychotherapyActivityResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.PsychotherapyActivity
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PsychotherapyActivityService(
    private val activityRepository: PsychotherapyActivityRepository,
    private val admissionRepository: AdmissionRepository,
    private val categoryRepository: PsychotherapyCategoryRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listActivities(admissionId: Long, sortDirection: String): List<PsychotherapyActivityResponse> {
        verifyAdmissionExists(admissionId)

        val activities = if (sortDirection.equals("asc", ignoreCase = true)) {
            activityRepository.findByAdmissionIdOrderByCreatedAtAsc(admissionId)
        } else {
            activityRepository.findByAdmissionIdOrderByCreatedAtDesc(admissionId)
        }

        // Batch fetch users to avoid N+1 queries
        val userIds = activities.mapNotNull { it.createdBy }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return activities.map { activity ->
            PsychotherapyActivityResponse.from(
                activity = activity,
                createdByUser = activity.createdBy?.let { users[it] },
            )
        }
    }

    @Transactional(readOnly = true)
    fun getActivity(admissionId: Long, activityId: Long): PsychotherapyActivityResponse {
        verifyAdmissionExists(admissionId)

        val activity = activityRepository.findByIdAndAdmissionId(activityId, admissionId)
            ?: throw ResourceNotFoundException(
                "Psychotherapy activity not found with id: $activityId for admission: $admissionId",
            )

        return buildResponse(activity)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun createActivity(admissionId: Long, request: CreatePsychotherapyActivityRequest): PsychotherapyActivityResponse {
        // Verify PSYCHOLOGIST role
        val currentUser = getCurrentUserDetails()
        if (!currentUser.hasRole("PSYCHOLOGIST")) {
            throw ForbiddenException("Only psychologists can register psychotherapy activities")
        }

        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        // Verify admission type is HOSPITALIZATION
        if (admission.type != AdmissionType.HOSPITALIZATION) {
            throw BadRequestException(
                "Psychotherapeutic activities can only be registered for hospitalized patients",
            )
        }

        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            ResourceNotFoundException("Psychotherapy category not found with id: ${request.categoryId}")
        }

        val activity = PsychotherapyActivity(
            admission = admission,
            category = category,
            description = request.description,
        )

        val saved = activityRepository.save(activity)
        return buildResponse(saved)
    }

    @Transactional
    fun deleteActivity(admissionId: Long, activityId: Long) {
        verifyAdmissionExists(admissionId)

        val activity = activityRepository.findByIdAndAdmissionId(activityId, admissionId)
            ?: throw ResourceNotFoundException(
                "Psychotherapy activity not found with id: $activityId for admission: $admissionId",
            )

        activity.deletedAt = LocalDateTime.now()
        activityRepository.save(activity)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun getCurrentUserDetails(): CustomUserDetails {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw UnauthorizedException("Not authenticated")
        return auth.principal as CustomUserDetails
    }

    private fun buildResponse(activity: PsychotherapyActivity): PsychotherapyActivityResponse {
        val createdByUser = activity.createdBy?.let { userRepository.findById(it).orElse(null) }
        return PsychotherapyActivityResponse.from(activity, createdByUser)
    }
}

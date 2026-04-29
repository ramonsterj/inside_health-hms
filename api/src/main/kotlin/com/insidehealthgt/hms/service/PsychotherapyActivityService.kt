package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreatePsychotherapyActivityRequest
import com.insidehealthgt.hms.dto.response.PsychotherapyActivityResponse
import com.insidehealthgt.hms.entity.AdmissionType
import com.insidehealthgt.hms.entity.PsychotherapyActivity
import com.insidehealthgt.hms.event.PsychotherapyActivityCreatedEvent
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.PsychotherapyActivityRepository
import com.insidehealthgt.hms.repository.PsychotherapyCategoryRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CurrentUserProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PsychotherapyActivityService(
    private val activityRepository: PsychotherapyActivityRepository,
    private val admissionRepository: AdmissionRepository,
    private val categoryRepository: PsychotherapyCategoryRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val messageService: MessageService,
    private val currentUserProvider: CurrentUserProvider,
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
                messageService.errorPsychotherapyActivityNotFound(activityId, admissionId),
            )

        return buildResponse(activity)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun createActivity(admissionId: Long, request: CreatePsychotherapyActivityRequest): PsychotherapyActivityResponse {
        // Verify PSYCHOLOGIST role
        val currentUser = currentUserProvider.currentUserDetailsOrThrow()
        if (!currentUser.hasRole("PSYCHOLOGIST")) {
            throw ForbiddenException(messageService.errorPsychotherapyActivityOnlyPsychologist())
        }

        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        // Verify admission type is HOSPITALIZATION
        if (admission.type != AdmissionType.HOSPITALIZATION) {
            throw BadRequestException(
                messageService.errorPsychotherapyActivityOnlyHospitalized(),
            )
        }

        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            ResourceNotFoundException(messageService.errorPsychotherapyCategoryNotFound(request.categoryId))
        }

        val activity = PsychotherapyActivity(
            admission = admission,
            category = category,
            description = request.description,
        )

        val saved = activityRepository.save(activity)

        // Publish event for billing if category has a price configured
        val price = category.price
        if (price != null && price > BigDecimal.ZERO) {
            eventPublisher.publishEvent(
                PsychotherapyActivityCreatedEvent(
                    admissionId = admission.id!!,
                    categoryName = category.name,
                    price = price,
                ),
            )
        }

        return buildResponse(saved)
    }

    @Transactional
    fun deleteActivity(admissionId: Long, activityId: Long) {
        verifyAdmissionExists(admissionId)

        val activity = activityRepository.findByIdAndAdmissionId(activityId, admissionId)
            ?: throw ResourceNotFoundException(
                messageService.errorPsychotherapyActivityNotFound(activityId, admissionId),
            )

        activity.deletedAt = LocalDateTime.now()
        activityRepository.save(activity)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))
        }
    }

    private fun buildResponse(activity: PsychotherapyActivity): PsychotherapyActivityResponse {
        val createdByUser = activity.createdBy?.let { userRepository.findById(it).orElse(null) }
        return PsychotherapyActivityResponse.from(activity, createdByUser)
    }
}

package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateVitalSignRequest
import com.insidehealthgt.hms.dto.request.UpdateVitalSignRequest
import com.insidehealthgt.hms.dto.response.VitalSignResponse
import com.insidehealthgt.hms.entity.Admission
import com.insidehealthgt.hms.entity.BaseEntity
import com.insidehealthgt.hms.entity.VitalSign
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ForbiddenException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.exception.UnauthorizedException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.repository.VitalSignRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Suppress("TooManyFunctions")
class VitalSignService(
    private val vitalSignRepository: VitalSignRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
    private val messageService: MessageService,
) {
    companion object {
        private const val EDIT_WINDOW_HOURS = 24L
        private const val CHART_MAX_RECORDS = 500
    }

    @Transactional(readOnly = true)
    fun listVitalSigns(
        admissionId: Long,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        pageable: Pageable,
    ): Page<VitalSignResponse> {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = getCurrentUserDetails()

        val vitalSigns = vitalSignRepository.findByAdmissionIdWithFilters(admissionId, fromDate, toDate, pageable)

        // Batch fetch users to avoid N+1 queries
        val userIds = vitalSigns.content.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return vitalSigns.map { vs ->
            VitalSignResponse.from(
                vitalSign = vs,
                createdByUser = vs.createdBy?.let { users[it] },
                updatedByUser = vs.updatedBy?.let { users[it] },
                canEdit = computeCanEdit(vs, currentUser, admission.isActive()),
            )
        }
    }

    @Transactional(readOnly = true)
    fun listVitalSignsForChart(admissionId: Long, fromDate: LocalDate?, toDate: LocalDate?): List<VitalSignResponse> {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = getCurrentUserDetails()

        val chartPageable = PageRequest.of(0, CHART_MAX_RECORDS, Sort.by(Sort.Direction.ASC, "recordedAt"))
        val vitalSigns = vitalSignRepository.findByAdmissionIdWithFilters(
            admissionId,
            fromDate,
            toDate,
            chartPageable,
        ).content

        // Batch fetch users to avoid N+1 queries
        val userIds = vitalSigns.flatMap { listOfNotNull(it.createdBy, it.updatedBy) }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return vitalSigns.map { vs ->
            VitalSignResponse.from(
                vitalSign = vs,
                createdByUser = vs.createdBy?.let { users[it] },
                updatedByUser = vs.updatedBy?.let { users[it] },
                canEdit = computeCanEdit(vs, currentUser, admission.isActive()),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getVitalSign(admissionId: Long, vitalSignId: Long): VitalSignResponse {
        val admission = getAdmissionOrThrow(admissionId)
        val currentUser = getCurrentUserDetails()

        val vitalSign = vitalSignRepository.findByIdAndAdmissionId(vitalSignId, admissionId)
            ?: throw ResourceNotFoundException(
                messageService.errorVitalSignNotFound(vitalSignId, admissionId),
            )

        return buildResponse(vitalSign, currentUser, admission.isActive())
    }

    @Transactional
    fun createVitalSign(admissionId: Long, request: CreateVitalSignRequest): VitalSignResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException(messageService.errorAdmissionNotFound(admissionId))

        validateAdmissionActive(admission)
        validateBloodPressure(request.systolicBp!!, request.diastolicBp!!)

        val recordedAt = request.recordedAt ?: LocalDateTime.now()
        validateRecordedAt(recordedAt, admission)

        val currentUser = getCurrentUserDetails()

        val vitalSign = VitalSign(
            admission = admission,
            recordedAt = recordedAt,
            systolicBp = request.systolicBp!!,
            diastolicBp = request.diastolicBp!!,
            heartRate = request.heartRate!!,
            respiratoryRate = request.respiratoryRate!!,
            temperature = request.temperature!!,
            oxygenSaturation = request.oxygenSaturation!!,
            glucose = request.glucose,
            other = request.other,
        )

        val saved = vitalSignRepository.save(vitalSign)
        return buildResponse(saved, currentUser, admission.isActive())
    }

    @Transactional
    fun updateVitalSign(admissionId: Long, vitalSignId: Long, request: UpdateVitalSignRequest): VitalSignResponse {
        val admission = getAdmissionOrThrow(admissionId)
        validateAdmissionActive(admission)
        validateBloodPressure(request.systolicBp!!, request.diastolicBp!!)

        val vitalSign = vitalSignRepository.findByIdAndAdmissionId(vitalSignId, admissionId)
            ?: throw ResourceNotFoundException(
                messageService.errorVitalSignNotFound(vitalSignId, admissionId),
            )

        val currentUser = getCurrentUserDetails()
        validateEditPermission(vitalSign, currentUser)

        val recordedAt = request.recordedAt ?: vitalSign.recordedAt
        validateRecordedAt(recordedAt, admission)

        vitalSign.recordedAt = recordedAt
        vitalSign.systolicBp = request.systolicBp!!
        vitalSign.diastolicBp = request.diastolicBp!!
        vitalSign.heartRate = request.heartRate!!
        vitalSign.respiratoryRate = request.respiratoryRate!!
        vitalSign.temperature = request.temperature!!
        vitalSign.oxygenSaturation = request.oxygenSaturation!!
        vitalSign.glucose = request.glucose
        vitalSign.other = request.other

        val saved = vitalSignRepository.save(vitalSign)
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

    private fun validateBloodPressure(systolicBp: Int, diastolicBp: Int) {
        if (systolicBp <= diastolicBp) {
            throw BadRequestException(messageService.errorVitalSignSystolicGreaterThanDiastolic())
        }
    }

    private fun validateRecordedAt(recordedAt: LocalDateTime, admission: Admission) {
        if (recordedAt.isBefore(admission.admissionDate)) {
            throw BadRequestException(messageService.errorVitalSignRecordedAtBeforeAdmission())
        }
        if (recordedAt.isAfter(LocalDateTime.now())) {
            throw BadRequestException(messageService.errorVitalSignRecordedAtFuture())
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
        vitalSign: VitalSign,
        currentUser: CustomUserDetails,
        admissionActive: Boolean,
    ): VitalSignResponse {
        val userIds = listOfNotNull(vitalSign.createdBy, vitalSign.updatedBy).distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return VitalSignResponse.from(
            vitalSign = vitalSign,
            createdByUser = vitalSign.createdBy?.let { users[it] },
            updatedByUser = vitalSign.updatedBy?.let { users[it] },
            canEdit = computeCanEdit(vitalSign, currentUser, admissionActive),
        )
    }
}

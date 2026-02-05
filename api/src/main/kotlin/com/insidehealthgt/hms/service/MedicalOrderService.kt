package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateMedicalOrderRequest
import com.insidehealthgt.hms.dto.request.UpdateMedicalOrderRequest
import com.insidehealthgt.hms.dto.response.GroupedMedicalOrdersResponse
import com.insidehealthgt.hms.dto.response.MedicalOrderResponse
import com.insidehealthgt.hms.entity.MedicalOrder
import com.insidehealthgt.hms.entity.MedicalOrderStatus
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.MedicalOrderRepository
import com.insidehealthgt.hms.repository.UserRepository
import com.insidehealthgt.hms.security.CustomUserDetails
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MedicalOrderService(
    private val medicalOrderRepository: MedicalOrderRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun listMedicalOrders(admissionId: Long): GroupedMedicalOrdersResponse {
        verifyAdmissionExists(admissionId)

        val orders = medicalOrderRepository.findByAdmissionIdWithRelations(admissionId)

        // Batch fetch users to avoid N+1 queries
        val userIds = orders.flatMap {
            listOfNotNull(it.createdBy, it.updatedBy, it.discontinuedBy)
        }.distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        val groupedOrders = orders
            .map { order ->
                MedicalOrderResponse.from(
                    medicalOrder = order,
                    createdByUser = order.createdBy?.let { users[it] },
                    updatedByUser = order.updatedBy?.let { users[it] },
                    discontinuedByUser = order.discontinuedBy?.let { users[it] },
                )
            }
            .groupBy { it.category }

        return GroupedMedicalOrdersResponse(orders = groupedOrders)
    }

    @Transactional(readOnly = true)
    fun getMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        return buildResponse(order)
    }

    @Transactional
    fun createMedicalOrder(admissionId: Long, request: CreateMedicalOrderRequest): MedicalOrderResponse {
        val admission = admissionRepository.findByIdWithRelations(admissionId)
            ?: throw ResourceNotFoundException("Admission not found with id: $admissionId")

        val order = MedicalOrder(
            admission = admission,
            category = request.category,
            startDate = request.startDate,
            endDate = request.endDate,
            medication = request.medication,
            dosage = request.dosage,
            route = request.route,
            frequency = request.frequency,
            schedule = request.schedule,
            observations = request.observations,
        )

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun updateMedicalOrder(
        admissionId: Long,
        orderId: Long,
        request: UpdateMedicalOrderRequest,
    ): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status == MedicalOrderStatus.DISCONTINUED) {
            throw BadRequestException("Cannot update a discontinued medical order")
        }

        order.category = request.category
        order.startDate = request.startDate
        order.endDate = request.endDate
        order.medication = request.medication
        order.dosage = request.dosage
        order.route = request.route
        order.frequency = request.frequency
        order.schedule = request.schedule
        order.observations = request.observations

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    @Transactional
    fun discontinueMedicalOrder(admissionId: Long, orderId: Long): MedicalOrderResponse {
        verifyAdmissionExists(admissionId)

        val order = medicalOrderRepository.findByIdAndAdmissionId(orderId, admissionId)
            ?: throw ResourceNotFoundException("Medical order not found with id: $orderId for admission: $admissionId")

        if (order.status == MedicalOrderStatus.DISCONTINUED) {
            throw BadRequestException("Medical order is already discontinued")
        }

        val currentUserId = getCurrentUserId()

        order.status = MedicalOrderStatus.DISCONTINUED
        order.discontinuedAt = LocalDateTime.now()
        order.discontinuedBy = currentUserId

        val saved = medicalOrderRepository.save(order)
        return buildResponse(saved)
    }

    private fun verifyAdmissionExists(admissionId: Long) {
        if (!admissionRepository.existsById(admissionId)) {
            throw ResourceNotFoundException("Admission not found with id: $admissionId")
        }
    }

    private fun getCurrentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
        val principal = auth?.principal
        return if (principal is CustomUserDetails) {
            principal.id
        } else {
            throw IllegalStateException("Unable to get current user ID")
        }
    }

    private fun buildResponse(order: MedicalOrder): MedicalOrderResponse {
        val userIds = listOfNotNull(order.createdBy, order.updatedBy, order.discontinuedBy).distinct()
        val users = if (userIds.isNotEmpty()) {
            userRepository.findAllById(userIds).associateBy { it.id!! }
        } else {
            emptyMap()
        }

        return MedicalOrderResponse.from(
            medicalOrder = order,
            createdByUser = order.createdBy?.let { users[it] },
            updatedByUser = order.updatedBy?.let { users[it] },
            discontinuedByUser = order.discontinuedBy?.let { users[it] },
        )
    }
}

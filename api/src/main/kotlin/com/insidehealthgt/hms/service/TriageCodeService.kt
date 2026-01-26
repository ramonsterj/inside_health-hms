package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.request.CreateTriageCodeRequest
import com.insidehealthgt.hms.dto.request.UpdateTriageCodeRequest
import com.insidehealthgt.hms.dto.response.TriageCodeResponse
import com.insidehealthgt.hms.entity.TriageCode
import com.insidehealthgt.hms.exception.BadRequestException
import com.insidehealthgt.hms.exception.ResourceNotFoundException
import com.insidehealthgt.hms.repository.AdmissionRepository
import com.insidehealthgt.hms.repository.TriageCodeRepository
import com.insidehealthgt.hms.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TriageCodeService(
    private val triageCodeRepository: TriageCodeRepository,
    private val admissionRepository: AdmissionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun findById(id: Long): TriageCode = triageCodeRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Triage code not found with id: $id") }

    @Transactional(readOnly = true)
    fun findAll(): List<TriageCodeResponse> = triageCodeRepository.findAllByOrderByDisplayOrderAsc()
        .map { TriageCodeResponse.fromSimple(it) }

    @Transactional(readOnly = true)
    fun getTriageCode(id: Long): TriageCodeResponse {
        val triageCode = findById(id)
        return buildTriageCodeResponse(triageCode)
    }

    @Transactional
    fun createTriageCode(request: CreateTriageCodeRequest): TriageCodeResponse {
        if (triageCodeRepository.existsByCode(request.code)) {
            throw BadRequestException("Triage code with code '${request.code}' already exists")
        }

        val triageCode = TriageCode(
            code = request.code,
            color = request.color,
            description = request.description,
            displayOrder = request.displayOrder,
        )

        val savedTriageCode = triageCodeRepository.save(triageCode)
        return buildTriageCodeResponse(savedTriageCode)
    }

    @Transactional
    fun updateTriageCode(id: Long, request: UpdateTriageCodeRequest): TriageCodeResponse {
        val triageCode = findById(id)

        if (triageCodeRepository.existsByCodeExcludingId(request.code, id)) {
            throw BadRequestException("Triage code with code '${request.code}' already exists")
        }

        triageCode.code = request.code
        triageCode.color = request.color
        triageCode.description = request.description
        triageCode.displayOrder = request.displayOrder

        val savedTriageCode = triageCodeRepository.save(triageCode)
        return buildTriageCodeResponse(savedTriageCode)
    }

    @Transactional
    fun deleteTriageCode(id: Long) {
        val triageCode = findById(id)

        if (admissionRepository.existsActiveByTriageCodeId(id)) {
            throw BadRequestException("Cannot delete triage code that is in use by active admissions")
        }

        triageCode.deletedAt = LocalDateTime.now()
        triageCodeRepository.save(triageCode)
    }

    private fun buildTriageCodeResponse(triageCode: TriageCode): TriageCodeResponse {
        val createdByUser = triageCode.createdBy?.let { userRepository.findById(it).orElse(null) }
        val updatedByUser = triageCode.updatedBy?.let { userRepository.findById(it).orElse(null) }
        return TriageCodeResponse.from(triageCode, createdByUser, updatedByUser)
    }
}

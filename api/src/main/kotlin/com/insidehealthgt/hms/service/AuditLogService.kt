package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.AuditLogResponse
import com.insidehealthgt.hms.dto.response.AuditUserSummary
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.repository.AuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuditLogService(private val auditLogRepository: AuditLogRepository) {

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<AuditLogResponse> = auditLogRepository.findAll(pageable).map {
        AuditLogResponse.from(it)
    }

    @Transactional(readOnly = true)
    fun findByFilters(
        userId: Long?,
        entityType: String?,
        action: AuditAction?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        pageable: Pageable,
    ): Page<AuditLogResponse> =
        auditLogRepository.findByFilters(userId, entityType, action, startDate, endDate, pageable)
            .map { AuditLogResponse.from(it) }

    @Transactional(readOnly = true)
    fun findByEntity(entityType: String, entityId: Long, pageable: Pageable): Page<AuditLogResponse> =
        auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
            .map { AuditLogResponse.from(it) }

    @Transactional(readOnly = true)
    fun getDistinctEntityTypes(): List<String> = auditLogRepository.findDistinctEntityTypes()

    @Transactional(readOnly = true)
    fun getDistinctUsers(): List<AuditUserSummary> = auditLogRepository.findDistinctUsers()
}

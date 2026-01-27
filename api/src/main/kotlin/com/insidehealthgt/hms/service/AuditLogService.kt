package com.insidehealthgt.hms.service

import com.insidehealthgt.hms.dto.response.AuditLogResponse
import com.insidehealthgt.hms.entity.AuditAction
import com.insidehealthgt.hms.repository.AuditLogRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
        pageable: Pageable,
    ): Page<AuditLogResponse> = auditLogRepository.findByFilters(userId, entityType, action, pageable)
        .map { AuditLogResponse.from(it) }

    @Transactional(readOnly = true)
    fun findByUserId(userId: Long, pageable: Pageable): Page<AuditLogResponse> =
        auditLogRepository.findByUserId(userId, pageable)
            .map { AuditLogResponse.from(it) }

    @Transactional(readOnly = true)
    fun findByEntity(entityType: String, entityId: Long, pageable: Pageable): Page<AuditLogResponse> =
        auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
            .map { AuditLogResponse.from(it) }

    @Transactional(readOnly = true)
    fun getDistinctEntityTypes(): List<String> = auditLogRepository.findDistinctEntityTypes()
}

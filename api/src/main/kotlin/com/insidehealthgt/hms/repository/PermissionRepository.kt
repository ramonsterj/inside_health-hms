package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByCode(code: String): Permission?
    fun findByResource(resource: String): List<Permission>
    fun findAllByCodeIn(codes: Collection<String>): List<Permission>
}

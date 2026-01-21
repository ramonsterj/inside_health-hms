package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByCode(code: String): Role?
    fun existsByCode(code: String): Boolean
    fun findAllByCodeIn(codes: Collection<String>): List<Role>

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    fun findByIdWithPermissions(id: Long): Role?

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    fun findByCodeWithPermissions(code: String): Role?
}

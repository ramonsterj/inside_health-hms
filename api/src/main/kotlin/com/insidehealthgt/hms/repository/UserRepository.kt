package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.User
import com.insidehealthgt.hms.entity.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun findByEmailOrUsername(email: String, username: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByUsername(username: String): Boolean

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions WHERE u.id = :id")
    fun findByIdWithRolesAndPermissions(id: Long): User?

    @Query("SELECT u FROM User u WHERE (:status IS NULL OR u.status = :status)")
    fun findByStatus(@Param("status") status: UserStatus?, pageable: Pageable): Page<User>

    @Query(
        "SELECT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions " +
            "WHERE u.email = :identifier OR u.username = :identifier",
    )
    fun findByIdentifierWithRolesAndPermissions(identifier: String): User?

    @Query(
        value = "SELECT * FROM users WHERE deleted_at IS NOT NULL",
        countQuery = "SELECT COUNT(*) FROM users WHERE deleted_at IS NOT NULL",
        nativeQuery = true,
    )
    fun findAllDeleted(pageable: Pageable): Page<User>

    @Query(
        value = "SELECT * FROM users WHERE id = :id AND deleted_at IS NOT NULL",
        nativeQuery = true,
    )
    fun findDeletedById(id: Long): User?
}

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
@Suppress("TooManyFunctions")
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
        value = """
        SELECT DISTINCT u.* FROM users u
        LEFT JOIN user_roles ur ON u.id = ur.user_id
        LEFT JOIN roles r ON ur.role_id = r.id
        WHERE u.deleted_at IS NULL
        AND (:status IS NULL OR u.status = CAST(:status AS VARCHAR))
        AND (:roleCode IS NULL OR r.code = :roleCode)
        AND (:search IS NULL OR
            LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        countQuery = """
        SELECT COUNT(DISTINCT u.id) FROM users u
        LEFT JOIN user_roles ur ON u.id = ur.user_id
        LEFT JOIN roles r ON ur.role_id = r.id
        WHERE u.deleted_at IS NULL
        AND (:status IS NULL OR u.status = CAST(:status AS VARCHAR))
        AND (:roleCode IS NULL OR r.code = :roleCode)
        AND (:search IS NULL OR
            LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.last_name) LIKE LOWER(CONCAT('%', :search, '%')))
        """,
        nativeQuery = true,
    )
    fun findWithFilters(
        @Param("status") status: String?,
        @Param("roleCode") roleCode: String?,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<User>

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.phoneNumbers WHERE u.id = :id")
    fun findByIdWithPhoneNumbers(@Param("id") id: Long): User?

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

    @Query(
        """
        SELECT DISTINCT u FROM User u
        JOIN u.roles r
        WHERE r.code = :roleCode
        ORDER BY u.lastName, u.firstName
        """,
    )
    fun findByRoleCode(@Param("roleCode") roleCode: String): List<User>
}

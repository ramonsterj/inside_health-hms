package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.LabPanel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LabPanelRepository : JpaRepository<LabPanel, Long> {

    @Query(
        """
        SELECT DISTINCT p FROM LabPanel p
        LEFT JOIN FETCH p.items i
        LEFT JOIN FETCH i.labTest
        ORDER BY p.name ASC
        """,
    )
    fun findAllWithItems(): List<LabPanel>

    @Query(
        """
        SELECT p FROM LabPanel p
        LEFT JOIN FETCH p.items i
        LEFT JOIN FETCH i.labTest
        WHERE p.id = :id
        """,
    )
    fun findByIdWithItems(@Param("id") id: Long): LabPanel?

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean
}

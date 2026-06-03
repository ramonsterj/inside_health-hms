package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.LabTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LabTestRepository : JpaRepository<LabTest, Long> {

    fun findAllByOrderByNameAsc(): List<LabTest>

    fun findAllByActiveTrueOrderByNameAsc(): List<LabTest>

    fun findAllByIdInAndActiveTrue(ids: Collection<Long>): List<LabTest>

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean

    /**
     * True if any provider-test or panel item (including soft-deleted) references this
     * canonical test. Native query to bypass the @SQLRestriction soft-delete filter.
     */
    @Query(
        value = """
        SELECT EXISTS(
            SELECT 1 FROM lab_provider_tests WHERE lab_test_id = :labTestId
            UNION ALL
            SELECT 1 FROM lab_panel_items WHERE lab_test_id = :labTestId
        )
        """,
        nativeQuery = true,
    )
    fun existsReferencesByLabTestIdIncludingDeleted(labTestId: Long): Boolean
}

package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.LabProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface LabProviderRepository : JpaRepository<LabProvider, Long> {

    fun findAllByOrderByNameAsc(): List<LabProvider>

    fun findAllByActiveTrueOrderByNameAsc(): List<LabProvider>

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean

    /**
     * True if any provider-test (including soft-deleted) references this provider.
     * Native query to bypass the @SQLRestriction soft-delete filter.
     */
    @Query(
        value = "SELECT EXISTS(SELECT 1 FROM lab_provider_tests WHERE provider_id = :providerId)",
        nativeQuery = true,
    )
    fun existsProviderTestsByProviderIdIncludingDeleted(providerId: Long): Boolean
}

package com.insidehealthgt.hms.repository

import com.insidehealthgt.hms.entity.LabProviderTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LabProviderTestRepository : JpaRepository<LabProviderTest, Long> {

    /** A provider's active offerings — feeds the order form and panel resolution. */
    fun findByProviderIdAndActiveTrueOrderByDisplayNameAsc(providerId: Long): List<LabProviderTest>

    /** All of a provider's offerings (incl. inactive) — admin catalog view. */
    fun findByProviderIdOrderByDisplayNameAsc(providerId: Long): List<LabProviderTest>

    /** Active offerings by id — order-create validation (rejects inactive/deleted). */
    fun findAllByIdInAndActiveTrue(ids: Collection<Long>): List<LabProviderTest>

    fun existsByProviderIdAndLabTestIdAndDeletedAtIsNull(providerId: Long, labTestId: Long): Boolean
}
